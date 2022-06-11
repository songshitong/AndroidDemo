global main

main:
    mov ebx, 1
    mov ecx, 2
    add ebx, ecx

    mov [sui_bian_xie], ebx
    mov eax, [sui_bian_xie]

    ret

section .data
sui_bian_xie   dw    0