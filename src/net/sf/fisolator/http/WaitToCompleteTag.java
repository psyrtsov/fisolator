package net.sf.fisolator.http;

import net.sf.fisolator.FaultIsolator;

import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletRequest;
import java.util.logging.Logger;

/**
 * User: Pavel Syrtsov
 * Date: Oct 4, 2008
 * Time: 9:05:23 PM
 * todo: provide comments
 */
public class WaitToCompleteTag extends TagSupport {
    Logger log = Logger.getLogger("fisolator");
    private long totalWait = 0L;

    public String getTotalWait() {
        return Long.toString(totalWait);
    }

    public void setTotalWait(String totalWait) {
        this.totalWait = Long.parseLong(totalWait);
    }

    public int doStartTag() throws JspException {
        if (this.totalWait <= 0L) {
            throw new JspException(WaitToCompleteTag.class.getSimpleName() + ".totalWait attribute has wrong value");
        }
        ServletRequest request = pageContext.getRequest();
        FaultIsolator asyncFaultIsolator = ServletFaultIsolator.getFaultIsolatorIfExists(request);
        if (asyncFaultIsolator != null) {
            try {
                asyncFaultIsolator.waitToComplete(totalWait);
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
        }
        return SKIP_BODY;
    }

    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }
}
