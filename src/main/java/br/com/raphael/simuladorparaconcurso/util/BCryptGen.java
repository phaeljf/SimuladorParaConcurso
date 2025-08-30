package br.com.raphael.simuladorparaconcurso.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptGen {
    public static void main(String[] args) {
        var enc = new BCryptPasswordEncoder(10); // custo 10 (ok para dev)
        String senha = "admin123";               // coloque a senha que você quer
        System.out.println(enc.encode(senha));
    }
}
