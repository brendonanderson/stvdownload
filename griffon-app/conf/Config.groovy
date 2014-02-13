log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d [%t] %-5p %c - %m%n')
    }

    error  'org.codehaus.griffon',
            "org.apache.http",
            "groovyx.net.http.HTTPBuilder"

    info   'griffon.util',
           'griffon.core',
           'griffon.swing',
           'griffon.app'
}

