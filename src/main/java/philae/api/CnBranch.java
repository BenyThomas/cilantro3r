
package philae.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for cnBranch complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="cnBranch"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="buCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="buId" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/&gt;
 *         &lt;element name="buName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="glPrefix" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cnBranch", propOrder = {
    "buCode",
    "buId",
    "buName",
    "glPrefix",
    "status"
})
public class CnBranch {

    protected String buCode;
    protected Long buId;
    protected String buName;
    protected String glPrefix;
    protected String status;

    /**
     * Gets the value of the buCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBuCode() {
        return buCode;
    }

    /**
     * Sets the value of the buCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBuCode(String value) {
        this.buCode = value;
    }

    /**
     * Gets the value of the buId property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getBuId() {
        return buId;
    }

    /**
     * Sets the value of the buId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setBuId(Long value) {
        this.buId = value;
    }

    /**
     * Gets the value of the buName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBuName() {
        return buName;
    }

    /**
     * Sets the value of the buName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBuName(String value) {
        this.buName = value;
    }

    /**
     * Gets the value of the glPrefix property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGlPrefix() {
        return glPrefix;
    }

    /**
     * Sets the value of the glPrefix property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGlPrefix(String value) {
        this.glPrefix = value;
    }

    /**
     * Gets the value of the status property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStatus(String value) {
        this.status = value;
    }

}
