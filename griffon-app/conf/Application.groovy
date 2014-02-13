application {
    title = 'Simpletv'
    startupGroups = ['simpletv']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = true

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "simpletv"
    'simpletv' {
        model      = 'simpletv.SimpletvModel'
        view       = 'simpletv.SimpletvView'
        controller = 'simpletv.SimpletvController'
    }

}
