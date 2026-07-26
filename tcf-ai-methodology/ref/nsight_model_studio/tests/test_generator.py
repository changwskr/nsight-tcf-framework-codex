from __future__ import annotations

import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
import sys
sys.path.insert(0, str(ROOT))

from generator import generate_workspace, write_workspace  # noqa: E402
from validators import has_errors, validate_model, validate_workspace  # noqa: E402


class GeneratorTest(unittest.TestCase):
    def setUp(self) -> None:
        self.model = json.loads((ROOT / "sample_model.json").read_text(encoding="utf-8"))

    def test_sample_model_is_valid(self) -> None:
        issues = validate_model(self.model)
        self.assertFalse(has_errors(issues), issues)

    def test_expected_artifacts_are_generated(self) -> None:
        artifacts = generate_workspace([self.model])
        expected = {
            "src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvCustomerHandler.java",
            "src/main/java/com/nh/nsight/marketing/sv/entry/facade/SvCustomerFacade.java",
            "src/main/java/com/nh/nsight/marketing/sv/application/service/SvCustomerService.java",
            "src/main/java/com/nh/nsight/marketing/sv/application/rule/SvCustomerRule.java",
            "src/main/java/com/nh/nsight/marketing/sv/persistence/dao/SvCustomerDao.java",
            "src/main/java/com/nh/nsight/marketing/sv/persistence/mapper/SvCustomerMapper.java",
            "src/main/resources/mapper/sv/SvCustomerMapper.xml",
            "docs/TRACEABILITY_MATRIX.csv",
        }
        self.assertTrue(expected.issubset(artifacts.keys()))
        self.assertIn("serviceIds()", artifacts[next(p for p in artifacts if p.endswith("Handler.java"))])
        self.assertIn("SV.Customer.selectSummary", artifacts[next(p for p in artifacts if p.endswith("Handler.java"))])
        self.assertNotIn("AS customerNo,", artifacts["src/main/resources/mapper/sv/SvCustomerMapper.xml"])

    def test_domain_handler_merges_multiple_service_ids(self) -> None:
        model2 = json.loads(json.dumps(self.model))
        model2.update({
            "id": "sample-sv-customer-list",
            "aggregateName": "CustomerList",
            "operation": "SELECT_LIST",
            "methodName": "selectCustomerList",
            "screenId": "SV-CUS-0002",
            "screenName": "고객 목록 조회",
            "eventId": "SV-CUS-0002-E01",
            "eventName": "고객목록 조회",
            "serviceId": "SV.Customer.selectList",
            "transactionCode": "SV-INQ-0002",
        })
        artifacts = generate_workspace([self.model, model2])
        handlers = [content for path, content in artifacts.items() if path.endswith("SvCustomerHandler.java")]
        self.assertEqual(1, len(handlers))
        self.assertIn("SV.Customer.selectSummary", handlers[0])
        self.assertIn("SV.Customer.selectList", handlers[0])
        self.assertIn("case SELECT_CUSTOMER_LIST", handlers[0])

    def test_workspace_duplicate_service_id_is_blocked(self) -> None:
        model2 = json.loads(json.dumps(self.model))
        model2["id"] = "another"
        issues = validate_workspace([self.model, model2])
        self.assertTrue(any(item["code"] == "WS-001" for item in issues))

    def test_generated_main_java_compiles_with_contract_stubs(self) -> None:
        if not shutil.which("javac"):
            self.skipTest("javac is not installed")
        with tempfile.TemporaryDirectory() as temp:
            target = Path(temp)
            write_workspace([self.model], target)
            self._write_stubs(target)
            java_files = [str(path) for path in (target / "src/main/java").rglob("*.java")]
            result = subprocess.run(
                ["javac", "-encoding", "UTF-8", "-d", str(target / "classes"), *java_files],
                capture_output=True,
                text=True,
                check=False,
            )
            self.assertEqual(0, result.returncode, result.stderr)

    @staticmethod
    def _write_stubs(root: Path) -> None:
        stubs = {
            "org/springframework/stereotype/Component.java": "package org.springframework.stereotype; public @interface Component {}",
            "org/springframework/stereotype/Service.java": "package org.springframework.stereotype; public @interface Service {}",
            "org/springframework/stereotype/Repository.java": "package org.springframework.stereotype; public @interface Repository {}",
            "org/springframework/transaction/annotation/Transactional.java": "package org.springframework.transaction.annotation; public @interface Transactional { boolean readOnly() default false; int timeout() default -1; }",
            "org/springframework/util/StringUtils.java": "package org.springframework.util; public final class StringUtils { public static boolean hasText(String v){return v != null && !v.isBlank();} }",
            "org/apache/ibatis/annotations/Mapper.java": "package org.apache.ibatis.annotations; public @interface Mapper {}",
            "com/nh/nsight/tcf/core/support/error/ErrorCode.java": "package com.nh.nsight.tcf.core.support.error; public enum ErrorCode { SERVICE_NOT_FOUND }",
            "com/nh/nsight/tcf/core/support/error/BusinessException.java": "package com.nh.nsight.tcf.core.support.error; public class BusinessException extends RuntimeException { public BusinessException(String c,String m){super(m);} public BusinessException(ErrorCode c,String m){super(m);} }",
            "com/nh/nsight/tcf/core/support/message/StandardHeader.java": "package com.nh.nsight.tcf.core.support.message; public class StandardHeader { public String getServiceId(){return null;} public String getGuid(){return null;} }",
            "com/nh/nsight/tcf/core/support/message/StandardRequest.java": "package com.nh.nsight.tcf.core.support.message; public class StandardRequest<T> { public T getBody(){return null;} public StandardHeader getHeader(){return null;} }",
            "com/nh/nsight/tcf/core/support/context/TransactionContext.java": "package com.nh.nsight.tcf.core.support.context; import com.nh.nsight.tcf.core.support.message.StandardHeader; public class TransactionContext { public StandardHeader getHeader(){return null;} }",
            "com/nh/nsight/tcf/core/support/transaction/TransactionHandler.java": "package com.nh.nsight.tcf.core.support.transaction; import java.util.Collection; import java.util.Map; import com.nh.nsight.tcf.core.support.context.TransactionContext; import com.nh.nsight.tcf.core.support.message.StandardRequest; public interface TransactionHandler { Collection<String> serviceIds(); Object doHandle(StandardRequest<Map<String,Object>> r, TransactionContext c); }",
        }
        base = root / "src/main/java"
        for relative, content in stubs.items():
            path = base / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")


if __name__ == "__main__":
    unittest.main()
