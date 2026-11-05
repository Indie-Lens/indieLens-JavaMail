package com.indieLens.inc;

// Processo auxiliar dos testes: adquire a trava e aguarda uma entrada, sem acessar banco ou SMTP.
public class SingletonEventProcess {

    public static void main(String[] args) throws Exception {
        SingletonEvent evento = SingletonEvent.getInstance();

        if (!evento.iniciar()) {
            System.out.println("OCUPADO");
            return;
        }

        try {
            System.out.println("INICIOU");
            System.out.flush();

            if (args.length > 0 && "segurar".equals(args[0])) {
                System.in.read();
            }
        } finally {
            evento.finalizar();
        }
    }
}
