global main


_get_out:
    mov eax, 1
    ret


fibo:
    cmp eax, 1
    je _get_out
    cmp eax, 2
    je _get_out

    mov edx, eax
    sub eax, 1
    call fibo
    mov ebx, eax

    mov eax, edx
    sub eax, 2
    call fibo
    mov ecx, eax

    mov eax, ebx
    add eax, ecx

    ret


main:
    mov eax, 7
    call fibo
    ret

