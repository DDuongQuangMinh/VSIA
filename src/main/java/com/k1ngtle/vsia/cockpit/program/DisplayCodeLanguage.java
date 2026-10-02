package com.k1ngtle.vsia.cockpit.program;

/** These are real service toolchains, not the existing console subset interpreter. */
public enum DisplayCodeLanguage {
    PYTHON("Python", ".py"), CPP("C++", ".cpp"), CSHARP("C#", ".cs"), C("C", ".c"), ASM("ASM x86-64 Linux", ".asm"), LUA("Lua", ".lua"), JAVA("Java", ".java"), RUST("Rust", ".rs"), RUBY("Ruby", ".rb");
    public final String label, extension;
    DisplayCodeLanguage(String label,String extension){this.label=label;this.extension=extension;}
    public String example(DisplayDesign design) {
        String json=design.json(),escaped=json.replace("\\","\\\\").replace("\"","\\\"");
        return switch(this) {
            case PYTHON -> "# Real Python: emit one VSIA scene as JSON on stdout.\nimport json\nscene = json.loads(\""+escaped+"\")\nprint(json.dumps(scene))\n";
            case CPP -> "#include <iostream>\nint main() { std::cout << \""+escaped+"\"; }\n";
            case CSHARP -> "using System;\nConsole.WriteLine(\""+escaped+"\");\n";
            case C -> "#include <stdio.h>\nint main(void) { puts(\""+escaped+"\"); return 0; }\n";
            case ASM -> "; NASM ELF64, Linux syscall ABI; no Minecraft/host memory access.\nglobal _start\nsection .rodata\nscene: db `"+json.replace("\\","\\\\").replace("`","\\`")+"`\nscene_len: equ $-scene\nsection .text\n_start:\n mov rax, 1\n mov rdi, 1\n lea rsi, [rel scene]\n mov rdx, scene_len\n syscall\n mov rax, 60\n xor rdi, rdi\n syscall\n";
            case LUA -> "-- Real Lua: output a JSON scene, no libraries required.\nio.write(\""+escaped+"\")\n";
            case JAVA -> "public class Main { public static void main(String[] args) {\n System.out.println(\""+escaped+"\");\n} }\n";
            case RUST -> "fn main() { println!(\"{}\", \""+escaped+"\"); }\n";
            case RUBY -> "# Real Ruby\nputs <<'SCENE'\n"+json+"\nSCENE\n";
        };
    }
}
