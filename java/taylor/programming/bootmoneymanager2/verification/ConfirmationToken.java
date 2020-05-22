package taylor.programming.bootmoneymanager2.verification;

import lombok.Getter;
import lombok.Setter;
import taylor.programming.bootmoneymanager2.model.UserAccount;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class ConfirmationToken {

    private int tokenId;
    private String confirmationToken;
    private String createdDate;
    private String username;

    public ConfirmationToken(UserAccount username){
       // username  = user.getUsername();
        this.username = username.getUsername();
        confirmationToken = UUID.randomUUID().toString();
        LocalDate date = LocalDate.now();
        createdDate = date.toString();
    }

    public ConfirmationToken(int tokenId, String confirmationToken, String createdDate, String username){
        this.tokenId = tokenId;
        this.confirmationToken = confirmationToken;
        this.createdDate = createdDate;
        this.username = username;
    }



}
