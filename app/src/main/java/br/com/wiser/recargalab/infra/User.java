package br.com.wiser.recargalab.infra;

/** Entidade de usuário. Também é usada (de propósito) como corpo do PUT /users/me — ver Lab 5. */
public class User {
    private Long id;
    private String username;
    private String nome;
    private String email;
    private String telefone;
    private String passwordHash;
    private String role;
    private Integer cashbackPercent;
    private String kycStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getCashbackPercent() { return cashbackPercent; }
    public void setCashbackPercent(Integer cashbackPercent) { this.cashbackPercent = cashbackPercent; }
    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
}
