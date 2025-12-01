
package philae.api;

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * <p>Java class for listTerminalsResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="listTerminalsResponse"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="return" type="{http://api.PHilae/}ltResponse" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "listTerminalsResponse", propOrder = {
    "_return"
})
public class ListTerminalsResponse implements Serializable {

    @XmlElement(name = "return")
    protected LtResponse _return;

    /**
     * Gets the value of the return property.
     * 
     * @return
     *     possible object is
     *     {@link LtResponse }
     *     
     */
    public LtResponse getReturn() {
        return _return;
    }

    /**
     * Sets the value of the return property.
     * 
     * @param value
     *     allowed object is
     *     {@link LtResponse }
     *     
     */
    public void setReturn(LtResponse value) {
        this._return = value;
    }

}
