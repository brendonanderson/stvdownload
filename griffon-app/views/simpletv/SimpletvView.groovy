package simpletv

import javax.swing.*
import java.awt.event.ItemEvent

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
        textField(columns: 10, constraints: "wrap", text: bind("username", source: model, mutual: true))
        label(text: "Password", constraints: "right")
        passwordField(columns: 10, constraints: "wrap", text: bind("password", source: model, mutual: true))
        button("Login", actionPerformed: controller.login)
        comboBox(
                constraints: "wrap",
                model: eventComboBoxModel(source: model.dvrs),
                selectedIndex: bind("selectedDvrIndex", source: model, mutual: true),
                itemStateChanged: { e ->
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        controller.getShows(e.source.selectedIndex)
                    }
                }
        )
        checkBox(label: "Local", selected: bind("useLocalUrl", source: model, mutual: true))
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
        textField(columns: 20, constraints: "wrap", text: bind("saveLocation", source: model, mutual: true))
        button("Download", constraints: "wrap", actionPerformed: controller.download)
        progressBar(value: bind{model.downloadPct}, minimum: 0, maximum: 100, string: bind{(model.downloadPct as Long) + "%"}, stringPainted: true)
    }
}
