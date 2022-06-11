global main

eax_plus_1s:
    add eax, 1
    ret

main:
    mov eax, 0
    call eax_plus_1s
    ret