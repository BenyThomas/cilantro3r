
package philae.api;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for tmRequest complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tmRequest"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://api.PHilae/}xaRequest"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="terminal" type="{http://api.PHilae/}axTerminal" minOccurs="0"/&gt;
 *         &lt;element name="accounts" type="{http://api.PHilae/}cnAccount" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tmRequest", propOrder = {
    "terminal",
    "accounts"
})
public class TmRequest
    extends XaRequest
{

    protected AxTerminal terminal;
    @XmlElement(nillable = true)
    protected List<CnAccount> accounts;

    /**
     * Gets the value of the terminal property.
     * 
     * @return
     *     possible object is
     *     {@link AxTerminal }
     *     
     */
    public AxTerminal getTerminal() {
        return terminal;
    }

    /**
     * Sets the value of the terminal property.
     * 
     * @param value
     *     allowed object is
     *     {@link AxTerminal }
     *     
     */
    public void setTerminal(AxTerminal value) {
        this.terminal = value;
    }

    /**
     * Gets the value of the accounts property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the accounts property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAccounts().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CnAccount }
     * 
     * 
     */
    public List<CnAccount> getAccounts() {
        if (accounts == null) {
            accounts = new ArrayList<CnAccount>();
        }
        return this.accounts;
    }

}
