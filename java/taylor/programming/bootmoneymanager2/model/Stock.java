package taylor.programming.bootmoneymanager2.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Stock {

    // == properties ==
    public String symbol;
    public double open;
    public double high;
    public double low;
    public double close;
    public double volume;
    public int sharePurchaseAmount;
    public int numberOfShares;
    public String date;
    String trueDate;

    // == constructors ==
     Stock(){

    }

}
