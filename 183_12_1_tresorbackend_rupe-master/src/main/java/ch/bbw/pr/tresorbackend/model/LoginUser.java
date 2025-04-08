package ch.bbw.pr.tresorbackend.model;

import jakarta.validation.constraints.NotEmpty;
import lombok.Value;

/**
 * LoginUser
 * DTO for Login
 * @author Martin Atanasov
 */
@Value
public class LoginUser {

    @NotEmpty(message="E-Mail is required.")
    private String email;

    @NotEmpty(message="Password is required.")
    private String password;
}
