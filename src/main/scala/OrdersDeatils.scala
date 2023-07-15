import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.functions.{col, expr, first, lead, min, row_number, when}
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.catalyst.plans._

object OrdersDeatils extends App {
//  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
    val spark = SparkSession.builder().master("local[*]").appName("OrdersDetails").getOrCreate()

    import spark.implicits._

    //Creating Schema for csv
    val ordersSchema = StructType(Array(
      StructField("Order_ID", IntegerType, true),
      StructField("User_Name", StringType, true),
      StructField("Order_Time", IntegerType, true),
      StructField("Order_Type", StringType, true),
      StructField("Quantity", IntegerType, true),
      StructField("Price", IntegerType, true)
    ))

    //Read the csv file by passing the defined schema
    val ordersDf = spark.read.format("csv").option("header", "false")
      .option("inferSchema", "true")
      .option("delimiter", ",")
      .schema(ordersSchema)
      .load("C:\\Users\\KALEBU\\IdeaProjects\\OrdersFX\\src\\main\\resources\\exampleOrders.csv")
    ordersDf.show(false)

    //Approach1

    //Segregating the orders based on the order_type(Buy,SELL)

    val buy_order = ordersDf.filter(ordersDf("Order_Type") === "BUY")
    val sell_orders = ordersDf.filter(ordersDf("Order_Type") === "SELL")

    //to avoid ambigious issue renaming the sell_orders schema with new names

    val sell_orders_new = sell_orders.withColumnRenamed("Order_ID", "sell_order_id")
      .withColumnRenamed("User_Name", "sell_user_name")
      .withColumnRenamed("Order_Time", "sell_order_time")
      .withColumnRenamed("Order_Type", "sell_order_type")
      .withColumnRenamed("Quantity", "sell_quantity")
      .withColumnRenamed("Price", "sell_price")

    //Matching the both sell and buy orders based on the quantity and fetching the resultant orders and fetching the required values based on the conditions
    val matchedRecords = buy_order.join(sell_orders_new, buy_order("Quantity") === sell_orders_new("sell_quantity"), "inner").withColumn("buy_id", when(buy_order("Order_Time") > sell_orders_new("sell_order_time"), buy_order("Order_ID")).otherwise(sell_orders_new("sell_order_id")))
      .withColumn("seller_id", when(sell_orders_new("sell_order_time") > buy_order("Order_Time"), buy_order("Order_ID")).otherwise(sell_orders_new("sell_order_id")))
      .withColumn("order_time", when(sell_orders_new("sell_order_time") > buy_order("Order_Time"), sell_orders_new("sell_order_time")).otherwise(buy_order("Order_Time")))
      .withColumn("price12", when(sell_orders_new("sell_order_id") > buy_order("Order_ID"), buy_order("Price")).otherwise(sell_orders_new("sell_price")))
      .select("buy_id", "seller_id", "order_time", "Quantity", "price12")

    //Displaying the result by calling action

    matchedRecords.show(false)
    matchedRecords.printSchema()
    matchedRecords.write.mode(SaveMode.Overwrite).save("C:\\Users\\KALEBU\\IdeaProjects\\OrdersFX\\target\\outputFile.csv")

    //    matchedRecords.write.csv("file:///D:/orderss")


    //approach 2

    //      //Based on the particular column ordering the data
    //      val windowFunc = Window.orderBy("Quantity")
    //
    //      //creating new columns and passing the next associated values based on the windowFunc
    //
    //      val renamedOrdersDf = ordersDF.withColumn("Next_Quantity",lead("Quantity",1).over(windowFunc))
    //                        .withColumn("Next_Order_Type",lead("Order_Type",1).over(windowFunc))
    //                        .withColumn("Next_Order_Id",lead("Order_ID",1).over(windowFunc))
    //                        .withColumn("Next_Order_Time",lead("Order_Time",1).over(windowFunc))
    //
    //      //based on the order_type condition creating a new column and placing 1 in to the column values
    //      val orderCheckDF = renamedOrdersDf.withColumn("orderCheckType", when(renamedOrdersDf("Order_Type") === "BUY" && renamedOrdersDf("Next_Order_Type") === "SELL" || renamedOrdersDf("Order_Type") === "SELL" && renamedOrdersDf("Next_Order_Type") === "BUY",1))
    //      //based on the quantity and ordercheck type fetching the values
    //      val resultantOrders = orderCheckDF.where(orderCheckDF("Quantity") === renamedOrdersDf("Next_Quantity") && orderCheckDF("orderCheckType") === 1)
    //      //selecting the output values
    //      val outputOrders = resultantOrders.select("Order_ID","Next_Order_Id","Next_Order_Time","Quantity","Price")
    //      outputOrders.show(false)

//  }
}
