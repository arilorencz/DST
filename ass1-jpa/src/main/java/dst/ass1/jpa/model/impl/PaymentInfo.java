package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.IPaymentInfo;
import dst.ass1.jpa.model.PaymentMethod;
import dst.ass1.jpa.util.Constants;

import javax.persistence.*;

@Entity
@Table(name = Constants.T_PAYMENT_INFO)
public class PaymentInfo implements IPaymentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String name;

    @Column
    private PaymentMethod method;

    @Column
    private boolean preferred;

    // getters and setters

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return method;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.method = method;
    }

    @Override
    public Boolean isPreferred() {
        return preferred;
    }

    @Override
    public void setPreferred(Boolean preferred) {
        this.preferred = preferred;
    }
}