package dst.ass1.jpa.model.impl;

import dst.ass1.jpa.model.IMoney;
import dst.ass1.jpa.model.IPaymentInfo;
import dst.ass1.jpa.model.ITripInfo;
import dst.ass1.jpa.model.ITripReceipt;
import dst.ass1.jpa.util.Constants;
import org.hibernate.annotations.Target;

import javax.persistence.*;

@Entity
@Table(name = Constants.T_TRIP_RECEIPT)
public class TripReceipt implements ITripReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Embedded
    @Target(Money.class)
    private IMoney total;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = Constants.AO_NAME_TIP_CURRENCY,
                    column = @Column(name = Constants.AO_COLUMN_NAME_TIP_CURRENCY)
            ),
            @AttributeOverride(
                    name = Constants.AO_NAME_TIP_CURRENCY_VALUE,
                    column = @Column(name = Constants.AO_COLUMN_NAME_TIP_CURRENCY_VALUE)
            )
    })
    @Target(Money.class)
    private IMoney tip;

    @Column
    private Boolean paid;

    @ManyToOne(targetEntity = PaymentInfo.class)
    private IPaymentInfo paymentInfo;

    @OneToOne(targetEntity = TripInfo.class)
    private ITripInfo tripInfo;

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
    public IMoney getTotal() {
        return total;
    }

    @Override
    public void setTotal(IMoney total) {
        this.total = (Money) total;
    }

    @Override
    public IMoney getTip() {
        return tip;
    }

    @Override
    public void setTip(IMoney tip) {
        this.tip = (Money) tip;
    }

    @Override
    public boolean isPaid() {
        return paid;
    }

    @Override
    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    @Override
    public IPaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    @Override
    public void setPaymentInfo(IPaymentInfo paymentInfo) {
        this.paymentInfo = paymentInfo;
    }

    @Override
    public ITripInfo getTripInfo() {
        return tripInfo;
    }

    @Override
    public void setTripInfo(ITripInfo tripInfo) {
        this.tripInfo = tripInfo;
    }
}
