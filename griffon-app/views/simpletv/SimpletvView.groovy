package simpletv

import javax.swing.ListSelectionModel

application(title: 'simpletv',
  pack: true,
  locationByPlatform: true,
  iconImage:   imageIcon('/griffon-icon-48x48.png').image,
  iconImages: [imageIcon('/griffon-icon-48x48.png').image,
               imageIcon('/griffon-icon-32x32.png').image,
               imageIcon('/griffon-icon-16x16.png').image]) {
    migLayout(layoutConstraints: "fill")
    panel(constraints: "grow") {
        migLayout(layoutConstraints: "fill")
        label(text: "Username", constraints: "right")
        textField(columns: 10, constraints: "wrap", text: bind("username", target: model))
        label(text: "Password", constraints: "right")
        passwordField(columns: 10, constraints: "wrap", text: bind("password", target: model))
        button("Login", constraints: "wrap", actionPerformed: controller.login)
    }
    panel(constraints: "w 230!", border: titledBorder(title: "Shows")) {
        borderLayout()
        scrollPane() {
            list(constraints: "w 200!",
                    selectionMode: ListSelectionModel.SINGLE_SELECTION,
                    mouseClicked: { e ->
                        int index = e.source.locationToIndex(e.point)
                        if (index != -1) {
                            controller.getEpisodes(index)
                        }
                    },
                    model: eventListModel(source: model.shows)
            )
        }
    }
    panel(constraints: "w 300!", border: titledBorder(title: "Episodes")) {
        borderLayout()
        scrollPane() {
            list(constraints: "w 300!, grow",
                    selectionMode: ListSelectionModel.SINGLE_SELECTION,
                    model: eventListModel(source: model.episodes),
                    mouseClicked: { e->
                        int index = e.source.locationToIndex(e.point)
                        model.downloadPct = 0
                        if (index != -1) {
                            model.selectedEpisodeIndex = index
                            controller.getEpisodeUrls(index)
                        }
                    }
            )
        }
    }
    panel(constraints: "w 100!", border: titledBorder(title: "Quality")) {
        borderLayout()
        scrollPane() {
            list(constraints: "w 100!",
                    selectionMode: ListSelectionModel.SINGLE_SELECTION,
                    model: eventListModel(source: model.episodeUrls),
                    mouseClicked: { e ->
                        int index = e.source.locationToIndex(e.point)
                        model.downloadPct = 0
                        if (index != -1) {
                            model.selectedUrlIndex = index
                        }
                    }
            )
        }
    }
    panel(constraints: "wrap, grow") {
        migLayout()
        label(text: "Location", constraints: "right")
        textField(columns: 20, constraints: "wrap", text: bind{model.saveLocation})
        button("Download", constraints: "wrap", actionPerformed: controller.download)
        progressBar(value: bind{model.downloadPct}, minimum: 0, maximum: 100, string: bind{(model.downloadPct as Long) + "%"}, stringPainted: true)
    }
}
