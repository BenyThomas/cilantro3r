
package philae.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ltResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ltResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://api.PHilae/}xaResponse"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="terminals" minOccurs="0"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;sequence&gt;
 *                   &lt;element name="terminal" type="{http://api.PHilae/}axTerminal" maxOccurs="unbounded" minOccurs="0"/&gt;
 *                 &lt;/sequence&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ltResponse", propOrder = {
    "terminals"
})
@XmlRootElement(name = "return")
public class LtResponse
    extends XaResponse implements Serializable
{

    protected LtResponse.Terminals terminals;

    /**
     * Gets the value of the terminals property.
     * 
     * @return
     *     possible object is
     *     {@link LtResponse.Terminals }
     *     
     */
    public LtResponse.Terminals getTerminals() {
        return terminals;
    }

    /**
     * Sets the value of the terminals property.
     * 
     * @param value
     *     allowed object is
     *     {@link LtResponse.Terminals }
     *     
     */
    public void setTerminals(LtResponse.Terminals value) {
        this.terminals = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType&gt;
     *   &lt;complexContent&gt;
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
     *       &lt;sequence&gt;
     *         &lt;element name="terminal" type="{http://api.PHilae/}axTerminal" maxOccurs="unbounded" minOccurs="0"/&gt;
     *       &lt;/sequence&gt;
     *     &lt;/restriction&gt;
     *   &lt;/complexContent&gt;
     * &lt;/complexType&gt;
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "terminal"
    })
    public static class Terminals implements Serializable {

        protected List<AxTerminal> terminal;

        /**
         * Gets the value of the terminal property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the terminal property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getTerminal().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link AxTerminal }
         * 
         * 
         */
        public List<AxTerminal> getTerminal() {
            if (terminal == null) {
                terminal = new ArrayList<AxTerminal>();
            }
            return this.terminal;
        }

    }

}
