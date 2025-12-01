
package philae.api;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for aqResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="aqResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://api.PHilae/}xaResponse"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="account" type="{http://api.PHilae/}cnAccount" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "aqResponse", propOrder = {
    "account"
})
public class AqResponse
    extends XaResponse
{

    protected CnAccount account;

    /**
     * Gets the value of the account property.
     * 
     * @return
     *     possible object is
     *     {@link CnAccount }
     *     
     */
    public CnAccount getAccount() {
        return account;
    }

    /**
     * Sets the value of the account property.
     * 
     * @param value
     *     allowed object is
     *     {@link CnAccount }
     *     
     */
    public void setAccount(CnAccount value) {
        this.account = value;
    }

}
