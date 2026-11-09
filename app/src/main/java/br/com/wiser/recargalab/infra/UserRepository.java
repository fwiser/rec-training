package br.com.wiser.recargalab.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final RowMapper<User> MAPPER = (rs, i) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setNome(rs.getString("nome"));
        u.setEmail(rs.getString("email"));
        u.setTelefone(rs.getString("telefone"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setCashbackPercent(rs.getInt("cashback_percent"));
        u.setKycStatus(rs.getString("kyc_status"));
        return u;
    };

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByUsername(String username) {
        return jdbc.query("SELECT * FROM users WHERE username = ?", MAPPER, username).stream().findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT * FROM users WHERE email = ?", MAPPER, email).stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        return jdbc.query("SELECT * FROM users WHERE id = ?", MAPPER, id).stream().findFirst();
    }

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM users ORDER BY id", MAPPER);
    }

    public void update(User u) {
        jdbc.update("""
                UPDATE users SET nome = ?, email = ?, telefone = ?, password_hash = ?,
                                 role = ?, cashback_percent = ?, kyc_status = ?
                WHERE id = ?""",
                u.getNome(), u.getEmail(), u.getTelefone(), u.getPasswordHash(),
                u.getRole(), u.getCashbackPercent(), u.getKycStatus(), u.getId());
    }
}
