#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
WORK="$ROOT/build/syntax-self-test"
STUB="$WORK/stubs"
OUT_MAIN="$WORK/main"
OUT_TEST="$WORK/test"
rm -rf "$WORK"
mkdir -p "$STUB/org/springframework/boot/autoconfigure" "$STUB/org/springframework/boot" \
  "$STUB/org/junit/jupiter/api/io" "$STUB/org/junit/jupiter/api" "$STUB/org/assertj/core/api" \
  "$OUT_MAIN" "$OUT_TEST"
cat > "$STUB/org/springframework/boot/CommandLineRunner.java" <<'JAVA'
package org.springframework.boot;
public interface CommandLineRunner { void run(String... args) throws Exception; }
JAVA
cat > "$STUB/org/springframework/boot/SpringApplication.java" <<'JAVA'
package org.springframework.boot;
public final class SpringApplication { public static Object run(Class<?> type, String... args) { return null; } }
JAVA
cat > "$STUB/org/springframework/boot/autoconfigure/SpringBootApplication.java" <<'JAVA'
package org.springframework.boot.autoconfigure;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpringBootApplication {}
JAVA
cat > "$STUB/org/junit/jupiter/api/Test.java" <<'JAVA'
package org.junit.jupiter.api;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test {}
JAVA
cat > "$STUB/org/junit/jupiter/api/BeforeEach.java" <<'JAVA'
package org.junit.jupiter.api;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeforeEach {}
JAVA
cat > "$STUB/org/junit/jupiter/api/io/TempDir.java" <<'JAVA'
package org.junit.jupiter.api.io;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface TempDir {}
JAVA
cat > "$STUB/org/assertj/core/api/ThrowingCallable.java" <<'JAVA'
package org.assertj.core.api;
@FunctionalInterface
public interface ThrowingCallable { void call() throws Throwable; }
JAVA
cat > "$STUB/org/assertj/core/api/GenericAssert.java" <<'JAVA'
package org.assertj.core.api;
import java.util.function.Function;
public class GenericAssert<T> {
  public GenericAssert<T> isEqualTo(Object expected) { return this; }
  public GenericAssert<T> exists() { return this; }
  public GenericAssert<T> hasSize(int size) { return this; }
  public GenericAssert<T> isInstanceOf(Class<?> type) { return this; }
  public GenericAssert<T> hasMessageContaining(String text) { return this; }
  public GenericAssert<T> isTrue() { return this; }
  public GenericAssert<T> isFalse() { return this; }
  public GenericAssert<T> isZero() { return this; }
  public GenericAssert<T> contains(Object... values) { return this; }
  public GenericAssert<T> containsExactly(Object... values) { return this; }
  public <E,R> GenericAssert<R> extracting(Function<E,R> extractor) { return new GenericAssert<>(); }
}
JAVA
cat > "$STUB/org/assertj/core/api/Assertions.java" <<'JAVA'
package org.assertj.core.api;
public final class Assertions {
  private Assertions() {}
  public static <T> GenericAssert<T> assertThat(T actual) { return new GenericAssert<>(); }
  public static GenericAssert<Throwable> assertThatThrownBy(ThrowingCallable callable) { return new GenericAssert<>(); }
}
JAVA
find "$STUB/org/springframework" "$ROOT/src/main/java" -name '*.java' -print0 \
  | xargs -0 javac -Xlint:all -encoding UTF-8 -d "$OUT_MAIN"
find "$STUB" "$ROOT/src/main/java" "$ROOT/src/test/java" -name '*.java' -print0 \
  | xargs -0 javac -encoding UTF-8 -d "$OUT_TEST"
printf '%s\n' 'MAIN_AND_TEST_SYNTAX_PASS'
