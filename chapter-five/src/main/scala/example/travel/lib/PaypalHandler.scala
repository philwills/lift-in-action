package example.travel {
package lib {
  
  import net.liftweb.common.{Loggable,Full,Box,Empty,Failure}
  import net.liftweb.paypal.{PaypalIPN,PaypalPDT,PaypalTransactionStatus,PayPalInfo}
  import net.liftweb.http.DoRedirectResponse
  import net.liftweb.mapper.By
  import example.travel.model.{Order,OrderStatus}
  
  /**
   * The handler that implements the paypal integration from Lift. 
   * Note that you must register yourself on developer.paypal.com in order to 
   * get access to setup your paypal authentication tokens and use 
   * the IPN simulator.
   * 
   * Implementing both PDT and IPN is a belt and braces approach really; PDT is
   * generally considered to not be enough on its own, but can give you some immediate
   * information that you might otherwise have to wait for with IPN. Thus, implementing
   * them both lets us explore the integration nicely and get belt and braces.
   */
  object PaypalHandler extends PaypalIPN with PaypalPDT with Loggable {
    
    /**
     * Paypal PDT
     */
    val paypalAuthToken = "0Chc848Vqe3ztH0uzJqgl697_U6tLzajlcar4fEcbYSOzQTWsK4CEID_aFO"
    def pdtResponse = {
      case (info, resp) => resp.param("tx") match {
        case Full(token) => DoRedirectResponse.apply("/paypal/success")
        case _ => DoRedirectResponse.apply("/paypal/failure")
      }
    }
    
    /**
     * Paypal IPN
     */
    def actions = {
      case (PaypalTransactionStatus.CompletedPayment,info,_) => 
        updateOrder(info,OrderStatus.Complete)
      case (PaypalTransactionStatus.FailedPayment,info,_) => 
        updateOrder(info,OrderStatus.Failed)
      case (status, info, resp) =>
        logger.info("Got a PayPal IPN response of: " + status + ", with info: " + info)
    }
    
    private def updateOrder(info: PayPalInfo, status: OrderStatus.Value){
      Order.find(By(Order.reference, info.itemNumber.map(_.toLong).openOr(0L))) match {
        case Full(order) => order.status(status).save
        case _ =>
      }
    }
    
  }
  
}}