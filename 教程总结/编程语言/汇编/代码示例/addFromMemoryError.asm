global main

main:
    mov ebx, 1
    mov ecx, 2
    add ebx, ecx

    mov [0x233], ebx
    mov eax, [0x233]

    ret