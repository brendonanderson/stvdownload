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
        textField(columns: 10, constraints: "wrap", text: bind("username", source: model, mutual: true), enabled: bind("userNameEnabled", source: model, mutual: true))
        label(text: "Password", constraints: "right")
        passwordField(columns: 10, constraints: "wrap", text: bind("password", source: model, mutual: true), enabled: bind("passwordEnabled", source: model, mutual: true))
        button("Login", actionPerformed: controller.login, constraints: "wrap", enabled: bind("loginBtnEnabled", source: model, mutual: true))
        comboBox(
                constraints: "wrap, span 2",
                model: eventComboBoxModel(source: model.dvrs),
                selectedIndex: bind("selectedDvrIndex", source: model, mutual: true),
                itemStateChanged: { e ->
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        controller.getShows(e.source.selectedIndex)
                    }
                }
        )
        label(text: "Save Location", constraints: "right")
        textField(columns: 15, constraints: "wrap", text: bind("saveLocation", source: model, mutual: true))
        checkBox(label: "Local", selected: bind("useLocalUrl", source: model, mutual: true))
    }
    tabbedPane(constraints: "wrap") {
        panel(title: "Episodes") {
            migLayout()
            panel(border: titledBorder(title: "Shows"), constraints: "width 200!") {
                borderLayout()
                scrollPane() {
                    list(
                            selectionMode: ListSelectionModel.SINGLE_SELECTION,
                            mouseClicked: { e ->
                                if (model.showListEnabled) {
                                    int index = e.source.locationToIndex(e.point)
                                    if (index != -1) {
                                        model.selectedShowIndex = index
                                        controller.getEpisodes(index)
                                    }
                                }
                            },
                            model: eventListModel(source: model.shows),
                            enabled: bind("showListEnabled", source: model, mutual: true)
                    )
                }
            }
            panel(border: titledBorder(title: "Episodes"), constraints: "width 200!") {
                borderLayout()
                scrollPane() {
                    list(
                            selectionMode: ListSelectionModel.SINGLE_SELECTION,
                            model: eventListModel(source: model.episodes),
                            mouseClicked: { e->
                                if (model.episodeListEnabled) {
                                    int index = e.source.locationToIndex(e.point)
                                    model.downloadPct.value = 0
                                    if (index != -1) {
                                        model.selectedEpisodeIndex = index
                                        controller.getEpisodeUrls(index)
                                    }
                                }
                            },
                            enabled: bind("episodeListEnabled", source: model, mutual: true)
                    )
                }
            }
            panel(border: titledBorder(title: "Quality"), constraints: "width 85!") {
                borderLayout()
                scrollPane() {
                    list(
                            selectionMode: ListSelectionModel.SINGLE_SELECTION,
                            model: eventListModel(source: model.episodeUrls),
                            mouseClicked: { e ->
                                if (model.qualityListEnabled) {
                                    int index = e.source.locationToIndex(e.point)
                                    model.downloadPct.value = 0
                                    if (index != -1) {
                                        model.selectedUrlIndex = index
                                        model.downloadBtnEnabled = true
                                    }
                                }
                            },
                            enabled: bind("qualityListEnabled", source: model, mutual: true)
                    )
                }
            }
            panel() {
                migLayout()
                button("Download", constraints: "wrap", actionPerformed: controller.download, enabled: bind("downloadBtnEnabled", source: model, mutual: true))
                progressBar(value: bind{model.downloadPct.value}, minimum: 0, maximum: 100, string: bind{(model.downloadPct.value as Long) + "%"}, stringPainted: true)
            }
        }
/********************************************************************************/
        panel(title: "Batch") {
            migLayout()
            panel(border: titledBorder(title: "Shows"), constraints: "width 200!") {
                borderLayout()
                scrollPane() {
                    list(
                            selectionMode: ListSelectionModel.SINGLE_SELECTION,
                            mouseClicked: { e ->
                                if (model.batchShowListEnabled) {
                                    model.allEpisodesPct.value = 0
                                    model.batchPct.value = 0
                                    int index = e.source.locationToIndex(e.point)
                                    if (index != -1) {
                                        model.selectedBatchShowIndex = index
                                        model.downloadAllEnabled = true
                                    }
                                }
                            },
                            model: eventListModel(source: model.shows),
                            enabled: bind("batchShowListEnabled", source: model, mutual: true)
                    )
                }
            }
            panel() {
                migLayout()
                label(text: "Post batch command", constraints: "wrap")
                textField(columns: 20, constraints: "wrap", text: bind("postCommand", source: model, mutual: true), enabled: bind("postCommandEnabled", source: model, mutual: true))

            }
            panel() {
                migLayout()
                button("Download All", constraints: "wrap", actionPerformed: controller.downloadAll, enabled: bind("downloadAllEnabled", source: model, mutual: true))
                label(text: "Show Progress")
                progressBar(value: bind{model.allEpisodesPct.value}, minimum: 0, maximum: 100, string: bind{(model.allEpisodesPct.value as Long) + "%"}, stringPainted: true, constraints: "wrap")
                label(text: "Episode Progress")
                progressBar(value: bind{model.batchPct.value}, minimum: 0, maximum: 100, string: bind{(model.batchPct.value as Long) + "%"}, stringPainted: true, constraints: "wrap")
            }
        }
    }
    panel(constraints: "span 2") {
        label(text: " ")
        label(text: bind {model.status})
    }
}
