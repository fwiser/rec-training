package br.com.wiser.recargalab.security;

/** Nome do atributo de request onde o AuthFilter guarda o usuário autenticado. */
public final class CurrentUser {
    public static final String ATTR = "recargalab.user";

    private CurrentUser() {}
}
