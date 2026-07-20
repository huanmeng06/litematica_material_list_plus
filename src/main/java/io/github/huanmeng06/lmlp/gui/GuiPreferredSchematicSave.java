package io.github.huanmeng06.lmlp.gui;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.gui.GuiSchematicSave;
import fi.dy.masa.litematica.schematic.LitematicaSchematic;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message.MessageType;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.FileNameUtils;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicReplacement.ReplacementChoice;
import io.github.huanmeng06.lmlp.preference.PreferredSchematicPlacementApplication;
import net.minecraft.client.gui.screens.Screen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GuiPreferredSchematicSave extends GuiSchematicSave {
    private final LitematicaSchematic sourceSchematic;
    private final List<ReplacementChoice> choices;
    private final Path sourceFile;
    private final Path defaultDirectory;
    private final String initialFileName;
    private final SchematicPlacement sourcePlacement;
    private final Screen returnGui;
    private boolean initialFileNameApplied;

    public GuiPreferredSchematicSave(
            LitematicaSchematic sourceSchematic,
            List<ReplacementChoice> choices,
            Path sourceFile,
            String initialFileName,
            SchematicPlacement sourcePlacement,
            Screen returnGui) {
        super(sourceSchematic);
        this.sourceSchematic = sourceSchematic;
        this.choices = List.copyOf(choices);
        this.sourceFile = sourceFile;
        this.defaultDirectory = sourceFile != null && sourceFile.getParent() != null
                ? sourceFile.getParent()
                : DataManager.getSchematicsBaseDirectory();
        this.initialFileName = initialFileName;
        this.sourcePlacement = sourcePlacement;
        this.returnGui = returnGui;
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
        boolean destinationExists = Files.exists(destination);
        if (destinationExists && this.isSourceFile(destination)) {
            this.addMessage(MessageType.ERROR, "lmlp.error.preferred_replacement.source_file", fileName);
            return;
        }
        boolean overwrite = destinationExists && GuiBase.isShiftDown();
        if (destinationExists && !overwrite) {
            this.addMessage(MessageType.ERROR, "lmlp.error.preferred_replacement.file_exists", fileName);
            return;
        }

        LitematicaSchematic copy = PreferredSchematicReplacement.createCopy(this.sourceSchematic, this.choices);
        if (copy.writeToFile(directory, name, overwrite)) {
            copy.getMetadata().clearModifiedSinceSaved();
            this.getListWidget().refreshEntries();
            this.addMessage(MessageType.SUCCESS, "litematica.message.schematic_saved_as", name);
            LitematicaSchematic savedSchematic = PreferredSchematicPlacementApplication.savedSchematic(destination, copy);
            GuiBase.openGui(new GuiApplyPreferredSchematicConfirm(
                    this.sourcePlacement,
                    savedSchematic,
                    this.returnGui));
        } else {
            this.addMessage(MessageType.ERROR, "lmlp.error.preferred_replacement.save_failed", fileName);
        }
    }

    private boolean isSourceFile(Path destination) {
        if (this.sourceFile == null) {
            return false;
        }
        try {
            if (Files.exists(this.sourceFile) && Files.exists(destination)) {
                return Files.isSameFile(this.sourceFile, destination);
            }
        } catch (IOException ignored) {
            // Fall back to normalized absolute paths below.
        }
        return this.sourceFile.toAbsolutePath().normalize()
                .equals(destination.toAbsolutePath().normalize());
    }
}
