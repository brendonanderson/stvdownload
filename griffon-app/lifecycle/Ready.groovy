import simpletv.SimpletvModel

/*
 * This script is executed inside the UI thread, so be sure to  call
 * long running code in another thread.
 *
 * You have the following options
 * - execOutsideUI { // your code }
 * - execFuture { // your code }
 * - Thread.start { // your code }
 *
 * You have the following options to run code again inside the UI thread
 * - execInsideUIAsync { // your code }
 * - execInsideUISync { // your code }
 */

SimpletvModel stvmodel = app.getModels()["simpletv"] as SimpletvModel
Properties prop = new Properties()
File file = new File("stv.properties")
if (file.exists()) {
    prop.load(file.newDataInputStream())
    stvmodel.username = prop.getProperty("username")
    stvmodel.password = prop.getProperty("password")
}