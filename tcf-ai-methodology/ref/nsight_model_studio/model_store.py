from __future__ import annotations

import json
import threading
import uuid
from copy import deepcopy
from pathlib import Path
from typing import Any


class ModelStore:
    def __init__(self, data_file: Path, sample_file: Path):
        self.data_file = data_file
        self.sample_file = sample_file
        self._lock = threading.RLock()
        self.data_file.parent.mkdir(parents=True, exist_ok=True)
        if not self.data_file.exists():
            sample = json.loads(self.sample_file.read_text(encoding="utf-8"))
            self._write([sample])

    def _read(self) -> list[dict[str, Any]]:
        with self._lock:
            try:
                return json.loads(self.data_file.read_text(encoding="utf-8"))
            except (FileNotFoundError, json.JSONDecodeError):
                return []

    def _write(self, models: list[dict[str, Any]]) -> None:
        with self._lock:
            temp = self.data_file.with_suffix(".tmp")
            temp.write_text(json.dumps(models, ensure_ascii=False, indent=2), encoding="utf-8")
            temp.replace(self.data_file)

    def list(self) -> list[dict[str, Any]]:
        return self._read()

    def get(self, model_id: str) -> dict[str, Any] | None:
        return next((deepcopy(item) for item in self._read() if item.get("id") == model_id), None)

    def save(self, model: dict[str, Any]) -> dict[str, Any]:
        models = self._read()
        saved = deepcopy(model)
        saved["id"] = str(saved.get("id") or uuid.uuid4())
        index = next((i for i, item in enumerate(models) if item.get("id") == saved["id"]), None)
        if index is None:
            models.append(saved)
        else:
            models[index] = saved
        self._write(models)
        return deepcopy(saved)

    def delete(self, model_id: str) -> bool:
        models = self._read()
        filtered = [item for item in models if item.get("id") != model_id]
        if len(filtered) == len(models):
            return False
        self._write(filtered)
        return True

    def duplicate(self, model_id: str) -> dict[str, Any] | None:
        model = self.get(model_id)
        if not model:
            return None
        model["id"] = str(uuid.uuid4())
        model["serviceId"] = model.get("serviceId", "") + ".copy"
        model["transactionCode"] = ""
        model["eventId"] = ""
        model["methodName"] = model.get("methodName", "") + "Copy"
        model["aggregateName"] = model.get("aggregateName", "") + "Copy"
        return self.save(model)
