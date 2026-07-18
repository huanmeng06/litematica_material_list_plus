package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.FileNameUtils;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementChoice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GuiPreferredSchematicSave extends GuiSchematicSave {
    private final LitematicaSchematic sourceSchematic;
    private final List<ReplacementChoice> choices;
    private final Path defaultDirectory;
    private final String initialFileName;
    private boolean initialFileNameApplied;

    public GuiPreferredSchematicSave(
            LitematicaSchematic sourceSchematic,
            List<ReplacementChoice> choices,
            Path sourceFile,
            String initialFileName) {
        super(sourceSchematic);
        this.sourceSchematic = sourceSchematic;
        this.choices = List.copyOf(choices);
        this.defaultDirectory = sourceFile != null && sourceFile.getParent() != null
                ? sourceFile.getParent()
                : DataManager.getSchematicsBaseDirectory();
        this.initialFileName = initialFileName;
    }

    @Override
    public Path getDefaultDirectory() {
        return this.defaultDirectory;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!this.initialFileNameApplied) {
            this.setTextFieldText(this.initialFileName);
            this.initialFileNameApplied = true;
        }
    }

    @Override
    protected IButtonActionListener createButtonListener(ButtonType type) {
        return (button, mouseButton) -> this.saveNewCopy();
    }

    private void saveNewCopy() {
        if (this.getListWidget() == null) {
            return;
        }

        Path directory = this.getListWidget().getCurrentDirectory();
        String name = FileNameUtils.generateSimpleUnicodeSafeFileName(this.getTextFieldText());
        if (FileNameUtils.doesFileNameContainIllegalCharacters(name)) {
            name = FileNameUtils.generateSafeFileName(name);
        }
        if (!Files.isDirectory(directory)) {
            this.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_directory", directory.toAbsolutePath());
            return;
        }
        if (name.isEmpty()) {
            this.addMessage(MessageType.ERROR, "litematica.error.schematic_save.invalid_schematic_name", name);
            return;
        }

        String fileName = name.endsWith(LitematicaSchematic.FILE_EXTENSION)
                ? name
                : name + LitematicaSchematic.FILE_EXTENSION;
        Path destination = directory.resolve(fileName);
        if (Files.exists(destination)) {
            this.addMessage(MessageType.ERROR, "lmlp.error.preferred_replacement.file_exists", fileName);
            return;
        }

        LitematicaSchematic copy = PreferredSchematicReplacement.createCopy(this.sourceSchematic, this.choices);
        if (copy.writeToFile(directory, name, false)) {
            copy.getMetadata().clearModifiedSinceSaved();
            this.getListWidget().refreshEntries();
            this.addMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", name);
        } else {
            this.addMessage(MessageType.ERROR, "lmlp.error.preferred_replacement.save_failed", fileName);
        }
    }
}
