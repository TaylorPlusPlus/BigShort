package taylor.programming.bootmoneymanager2.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class UserAccount {

    // == properties ==
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String password;
    private double amountInSavings;
    private double amountInMargin;
    private int badge_collection_id;
    private int investment_collection_id;
    private Stock currentBrowsingStock;
    private String imageId;
    ArrayList<PurchasedStock> currentStockInvestments = new ArrayList<PurchasedStock>();
    ArrayList<PurchasedStock> currentStockInvestmentsShort = new ArrayList<PurchasedStock>();

    // == constructors ==
    public UserAccount(){

    }

    // == methods ==
    public void clearCurrentStocks(){
        currentStockInvestments.clear();
    }

    public void clearShortedStocks(){
        currentStockInvestmentsShort.clear();
    }


}
