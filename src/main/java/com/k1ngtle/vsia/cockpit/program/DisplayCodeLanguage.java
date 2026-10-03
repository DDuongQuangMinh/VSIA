package com.k1ngtle.vsia.cockpit.program;

/** These are real service toolchains, not the existing console subset interpreter. */
public enum DisplayCodeLanguage {
    PYTHON("Python", ".py"), CPP("C++", ".cpp"), CSHARP("C#", ".cs"), C("C", ".c"), ASM("ASM x86-64 Linux", ".asm"), LUA("Lua", ".lua"), JAVA("Java", ".java"), RUST("Rust", ".rs"), RUBY("Ruby", ".rb");
    public final String label, extension;
    DisplayCodeLanguage(String label,String extension){this.label=label;this.extension=extension;}
    private String q(String s){String value=new com.google.gson.JsonPrimitive(s).toString();if(this==RUBY)value=value.replace("#{","\\#{");if(this==LUA||this==RUST)value=value.replace("\\u2028","\\u{2028}").replace("\\u2029","\\u{2029}");return value;}
    public String example(DisplayDesign design){
        if(this==ASM)return assembly(design);
        StringBuilder code=new StringBuilder(switch(this){
            case PYTHON->"from vsia_display import Display\n\ndisplay = Display("+q(design.theme())+")\n";
            case CPP->"#include \"vsia_display.h\"\n\nint main() {\n    vsia_begin("+q(design.theme())+");\n";
            case C->"#include \"vsia_display.h\"\n\nint main(void) {\n    vsia_begin("+q(design.theme())+");\n";
            case CSHARP->"var display = new VsiaDisplay("+q(design.theme())+");\n";
            case LUA->"local Display = require(\"vsia_display\")\nlocal display = Display.new("+q(design.theme())+")\n";
            case JAVA->"public class Main {\n    public static void main(String[] args) {\n        VsiaDisplay display = new VsiaDisplay("+q(design.theme())+");\n";
            case RUST->"mod vsia_display;\nuse vsia_display::Display;\n\nfn main() {\n    let mut display = Display::new("+q(design.theme())+");\n";
            case RUBY->"require_relative 'vsia_display'\ndisplay = Display.new("+q(design.theme())+")\n";
            default->throw new IllegalStateException();
        });
        for(var w:design.widgets()){
            String args=q(w.type())+", "+w.x()+", "+w.y()+", "+w.w()+", "+w.h()+", "+w.variant()+", "+q(w.text())+", "+q(w.binding())+", "+w.section();
            code.append(switch(this){case PYTHON->"display.widget("+args+")\n";case CPP,C->"    vsia_widget("+args+");\n";case CSHARP->"display.Widget("+args+");\n";case LUA->"display:widget("+args+")\n";case JAVA->"        display.widget("+args+");\n";case RUST->"    display.widget("+args+");\n";case RUBY->"display.widget("+args+")\n";default->throw new IllegalStateException();});
        }
        code.append(switch(this){case PYTHON,RUBY->"display.emit()\n";case CPP,C->"    vsia_end();\n    return 0;\n}\n";case CSHARP->"display.Emit();\n";case LUA->"display:emit()\n";case JAVA->"        display.emit();\n    }\n}\n";case RUST->"    display.emit();\n}\n";default->throw new IllegalStateException();});return code.toString();
    }
    private static String asmString(String text){return "`"+text.replace("\\","\\\\").replace("`","\\`")+"`, 0";}
    private String assembly(DisplayDesign design){
        StringBuilder data=new StringBuilder("; NASM ELF64 + C display-builder ABI. Linux x86-64 registers.\nextern vsia_begin, vsia_widget, vsia_end\nglobal main\nsection .rodata\ntheme: db "+asmString(design.theme())+"\n");
        StringBuilder code=new StringBuilder("section .text\nmain:\n    push rbp\n    mov rbp, rsp\n    lea rdi, [rel theme]\n    call vsia_begin\n");int i=0;
        for(var w:design.widgets()){
            data.append("kind").append(i).append(": db ").append(asmString(w.type())).append("\ntext").append(i).append(": db ").append(asmString(w.text())).append("\nbinding").append(i).append(": db ").append(asmString(w.binding())).append('\n');
            code.append("    sub rsp, 32\n    lea rax, [rel text").append(i).append("]\n    mov [rsp], rax\n    lea rax, [rel binding").append(i).append("]\n    mov [rsp+8], rax\n    mov qword [rsp+16], ").append(w.section()).append("\n    lea rdi, [rel kind").append(i).append("]\n    mov esi, ").append(w.x()).append("\n    mov edx, ").append(w.y()).append("\n    mov ecx, ").append(w.w()).append("\n    mov r8d, ").append(w.h()).append("\n    mov r9d, ").append(w.variant()).append("\n    call vsia_widget\n    add rsp, 32\n");i++;
        }
        return data.append(code).append("    call vsia_end\n    xor eax, eax\n    pop rbp\n    ret\nsection .note.GNU-stack noalloc noexec nowrite progbits\n").toString();
    }
}
