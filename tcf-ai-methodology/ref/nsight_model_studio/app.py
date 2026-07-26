from __future__ import annotations

import json
import mimetypes
import os
import traceback
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

from generator import artifacts_to_zip, generate_workspace
from model_store import ModelStore
from validators import validate_model, validate_workspace

ROOT = Path(__file__).resolve().parent
STATIC = ROOT / "static"
STORE = ModelStore(ROOT / "data" / "models.json", ROOT / "sample_model.json")
HOST = os.environ.get("NSIGHT_MODEL_STUDIO_HOST", "127.0.0.1")
PORT = int(os.environ.get("NSIGHT_MODEL_STUDIO_PORT", "8787"))


class ApiError(Exception):
    def __init__(self, status: int, message: str):
        super().__init__(message)
        self.status = status
        self.message = message


class RequestHandler(BaseHTTPRequestHandler):
    server_version = "NSIGHTModelStudio/0.1"

    def log_message(self, fmt: str, *args) -> None:
        print(f"[{self.log_date_time_string()}] {self.address_string()} {fmt % args}")

    def _send_bytes(self, body: bytes, content_type: str, status: int = 200, headers: dict[str, str] | None = None) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        if headers:
            for key, value in headers.items():
                self.send_header(key, value)
        self.end_headers()
        self.wfile.write(body)

    def _json(self, data, status: int = 200) -> None:
        self._send_bytes(json.dumps(data, ensure_ascii=False, indent=2).encode("utf-8"), "application/json; charset=utf-8", status)

    def _read_json(self) -> dict:
        length = int(self.headers.get("Content-Length", "0"))
        if length <= 0:
            return {}
        raw = self.rfile.read(length)
        try:
            return json.loads(raw.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as exc:
            raise ApiError(400, f"JSON 요청을 해석할 수 없습니다: {exc}") from exc

    def _serve_static(self, path: str) -> None:
        relative = "index.html" if path in {"/", ""} else path.lstrip("/")
        target = (STATIC / relative).resolve()
        if STATIC.resolve() not in target.parents and target != STATIC.resolve():
            raise ApiError(403, "허용되지 않은 경로입니다.")
        if not target.exists() or not target.is_file():
            raise ApiError(404, "파일을 찾을 수 없습니다.")
        content_type = mimetypes.guess_type(target.name)[0] or "application/octet-stream"
        if content_type.startswith("text/") or content_type in {"application/javascript", "application/json"}:
            content_type += "; charset=utf-8"
        self._send_bytes(target.read_bytes(), content_type)

    def do_GET(self) -> None:
        try:
            parsed = urlparse(self.path)
            path = parsed.path
            if path == "/api/health":
                self._json({"status": "UP", "application": "NSIGHT Model Studio", "version": "0.1.0"})
                return
            if path == "/api/models":
                self._json({"models": STORE.list()})
                return
            if path.startswith("/api/models/"):
                model_id = path.split("/")[-1]
                model = STORE.get(model_id)
                if not model:
                    raise ApiError(404, "모델을 찾을 수 없습니다.")
                self._json(model)
                return
            if path == "/api/sample":
                self._json(json.loads((ROOT / "sample_model.json").read_text(encoding="utf-8")))
                return
            self._serve_static(path)
        except ApiError as exc:
            self._json({"error": exc.message}, exc.status)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": str(exc)}, 500)

    def do_POST(self) -> None:
        try:
            parsed = urlparse(self.path)
            path = parsed.path
            payload = self._read_json()

            if path == "/api/models":
                saved = STORE.save(payload)
                self._json(saved, 201)
                return
            if path == "/api/validate":
                model = payload.get("model") or payload
                issues = validate_model(model)
                self._json({"issues": issues, "errorCount": sum(i["level"] == "ERROR" for i in issues), "warningCount": sum(i["level"] == "WARNING" for i in issues)})
                return
            if path == "/api/validate-workspace":
                models = payload.get("models") or STORE.list()
                issues = []
                for model in models:
                    issues.extend(validate_model(model))
                issues.extend(validate_workspace(models))
                self._json({"issues": issues, "errorCount": sum(i["level"] == "ERROR" for i in issues), "warningCount": sum(i["level"] == "WARNING" for i in issues)})
                return
            if path == "/api/preview":
                models = payload.get("models") or [payload.get("model") or payload]
                artifacts = generate_workspace(models)
                selected = payload.get("path")
                if selected:
                    if selected not in artifacts:
                        raise ApiError(404, "미리보기 파일을 찾을 수 없습니다.")
                    self._json({"path": selected, "content": artifacts[selected], "paths": sorted(artifacts)})
                else:
                    default_path = next((p for p in sorted(artifacts) if p.endswith("Handler.java")), sorted(artifacts)[0])
                    self._json({"path": default_path, "content": artifacts[default_path], "paths": sorted(artifacts)})
                return
            if path == "/api/generate":
                models = payload.get("models") or [payload.get("model") or payload]
                artifacts = generate_workspace(models)
                zip_bytes = artifacts_to_zip(artifacts)
                filename = payload.get("filename") or "nsight-generated-workspace.zip"
                filename = "".join(ch for ch in filename if ch.isalnum() or ch in "-_.") or "nsight-generated-workspace.zip"
                self._send_bytes(
                    zip_bytes,
                    "application/zip",
                    headers={"Content-Disposition": f'attachment; filename="{filename}"'},
                )
                return
            if path == "/api/generate-saved":
                models = STORE.list()
                ids = payload.get("ids") or []
                if ids:
                    selected = {str(item) for item in ids}
                    models = [model for model in models if str(model.get("id")) in selected]
                artifacts = generate_workspace(models)
                zip_bytes = artifacts_to_zip(artifacts)
                self._send_bytes(
                    zip_bytes,
                    "application/zip",
                    headers={"Content-Disposition": 'attachment; filename="nsight-saved-models.zip"'},
                )
                return
            if path.startswith("/api/models/") and path.endswith("/duplicate"):
                model_id = path.split("/")[-2]
                duplicated = STORE.duplicate(model_id)
                if not duplicated:
                    raise ApiError(404, "복제할 모델을 찾을 수 없습니다.")
                self._json(duplicated, 201)
                return
            raise ApiError(404, "지원하지 않는 API입니다.")
        except ApiError as exc:
            self._json({"error": exc.message}, exc.status)
        except ValueError as exc:
            self._json({"error": str(exc)}, 422)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": str(exc)}, 500)

    def do_PUT(self) -> None:
        try:
            parsed = urlparse(self.path)
            path = parsed.path
            if not path.startswith("/api/models/"):
                raise ApiError(404, "지원하지 않는 API입니다.")
            model_id = path.split("/")[-1]
            payload = self._read_json()
            payload["id"] = model_id
            saved = STORE.save(payload)
            self._json(saved)
        except ApiError as exc:
            self._json({"error": exc.message}, exc.status)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": str(exc)}, 500)

    def do_DELETE(self) -> None:
        try:
            parsed = urlparse(self.path)
            path = parsed.path
            if not path.startswith("/api/models/"):
                raise ApiError(404, "지원하지 않는 API입니다.")
            model_id = path.split("/")[-1]
            if not STORE.delete(model_id):
                raise ApiError(404, "삭제할 모델을 찾을 수 없습니다.")
            self._json({"deleted": True})
        except ApiError as exc:
            self._json({"error": exc.message}, exc.status)
        except Exception as exc:  # noqa: BLE001
            traceback.print_exc()
            self._json({"error": str(exc)}, 500)


if __name__ == "__main__":
    server = ThreadingHTTPServer((HOST, PORT), RequestHandler)
    print(f"NSIGHT Model Studio 0.1.0: http://{HOST}:{PORT}")
    print("종료: Ctrl+C")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n종료합니다.")
    finally:
        server.server_close()
