package com.delphi.delphi.utils.payments;

import com.stripe.model.PaymentMethod;

public class StripePaymentMethod {
    private String brand;
    private String last4;

    public StripePaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod.getCard() != null) {
            this.brand = paymentMethod.getCard().getBrand();
            this.last4 = paymentMethod.getCard().getLast4();
        }
    }

    public String getBrand() {
        return brand;
    }

    public String getLast4() {
        return last4;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setLast4(String last4) {
        this.last4 = last4;
    }
    
}
