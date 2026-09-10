package com.k1ngtle.vsia.signality.internet.web;

public final class W129SelfTest {
    private W129SelfTest() {
    }

    public static String run() {
        int passed = 0;
        int failed = 0;
        StringBuilder out = new StringBuilder();

        if (testAssembly()) {
            passed++;
            out.append("[PASS] w129-assembly-vm\n");
        } else {
            failed++;
            out.append("[FAIL] w129-assembly-vm\n");
        }

        if (testPython()) {
            passed++;
            out.append("[PASS] w129-python-runner\n");
        } else {
            failed++;
            out.append("[FAIL] w129-python-runner\n");
        }

        if (testC()) {
            passed++;
            out.append("[PASS] w129-c-runner\n");
        } else {
            failed++;
            out.append("[FAIL] w129-c-runner\n");
        }

        if (testCpp()) {
            passed++;
            out.append("[PASS] w129-cpp-runner\n");
        } else {
            failed++;
            out.append("[FAIL] w129-cpp-runner\n");
        }

        if (testCSharp()) {
            passed++;
            out.append("[PASS] w129-csharp-runner\n");
        } else {
            failed++;
            out.append("[FAIL] w129-csharp-runner\n");
        }

        if (testJava()) {
            passed++;
            out.append("[PASS] w129-java-runner\n");
        } else {
            failed++;
            out.append("[FAIL] w129-java-runner\n");
        }

        out.append("W1.29 self-test result: ")
                .append(passed)
                .append(" passed, ")
                .append(failed)
                .append(" failed");

        return out.toString();
    }

    private static boolean testAssembly() {
        W129RunResult result =
                W129ComputeEngine.run(
                        "/asm/test.asm",
                        """
                        section .text
                        global _start
                        _start:
                            mov r0, 40
                            add r0, 2
                            print r0
                            halt
                        """
                );

        return result.success()
                && result.output().trim().equals("42");
    }

    private static boolean testPython() {
        W129RunResult result =
                W129ComputeEngine.run(
                        "/tools/test.py",
                        "print(\"python-ok\")"
                );

        return result.success()
                && result.output().contains("python-ok");
    }

    private static boolean testC() {
        W129RunResult result =
                W129ComputeEngine.run(
                        "/native/test.c",
                        "int main(void) {\nprintf(\"c-ok\\n\");\nreturn 0;\n}"
                );

        return result.success()
                && result.output().contains("c-ok");
    }

    private static boolean testCpp() {
        W129RunResult result =
                W129ComputeEngine.run(
                        "/native/test.cpp",
                        "int main() {\nstd::cout << \"cpp-ok\" << std::endl;\nreturn 0;\n}"
                );

        return result.success()
                && result.output().contains("cpp-ok");
    }

    private static boolean testCSharp() {
        W129RunResult result =
                W129ComputeEngine.run(
                        "/dotnet/Test.cs",
                        "Console.WriteLine(\"cs-ok\");"
                );

        return result.success()
                && result.output().contains("cs-ok");
    }

    private static boolean testJava() {
        W129RunResult result =
                W129ComputeEngine.run(
                        "/java/Test.java",
                        "System.out.println(\"java-ok\");"
                );

        return result.success()
                && result.output().contains("java-ok");
    }
}
