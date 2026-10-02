package com.k1ngtle.vsia.item;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class DisplayHardDriveItem extends Item {
    public static final String PROGRAM_F35_CREATE =
            "vsia:f35_create_display";

    public static final String PROGRAM_CUSTOM =
            "vsia:custom_display";

    private static final String TAG_PROGRAM =
            "VsiaDisplayProgram";

    private static final String TAG_SOURCE =
            "VsiaDisplaySource";

    private static final String TAG_NAME =
            "VsiaDisplayName";

    public static java.util.UUID driveId(ItemStack stack) {
        CompoundTag tag=stack.getTag();return tag!=null&&tag.hasUUID("VsiaDriveId")?tag.getUUID("VsiaDriveId"):null;
    }
    public static java.util.UUID ensureDriveId(ItemStack stack) {
        if(!isDisplayDrive(stack))throw new IllegalArgumentException("Not a display drive");
        java.util.UUID id=driveId(stack);if(id==null){id=java.util.UUID.randomUUID();stack.getOrCreateTag().putUUID("VsiaDriveId",id);}return id;
    }
    public static long revision(ItemStack stack){return stack.getTag()==null?0:stack.getTag().getLong("VsiaDriveRevision");}
    public static String layout(ItemStack stack){return stack.getTag()==null?"":stack.getTag().getString("VsiaDisplayLayout");}
    public static String language(ItemStack stack){return stack.getTag()==null?"PYTHON":stack.getTag().getString("VsiaDisplayLanguage");}
    public static boolean writable(ItemStack stack){return isDisplayDrive(stack)&&!((DisplayHardDriveItem)stack.getItem()).factoryProgrammed();}
    private static void changed(ItemStack stack){stack.getOrCreateTag().putLong("VsiaDriveRevision",revision(stack)+1);}
    public static void writeDesign(ItemStack stack,String name,String layout,String language,String source){
        if(!writable(stack))throw new IllegalArgumentException("Read-only drive");
        String validated=com.k1ngtle.vsia.cockpit.program.DisplayDesign.parse(layout).json();
        com.k1ngtle.vsia.cockpit.program.DisplayCodeLanguage.valueOf(language);
        if(name==null||name.length()>48||source==null||source.length()>24576)throw new IllegalArgumentException("Program too large");
        setCustomSource(stack,name,source);CompoundTag tag=stack.getOrCreateTag();tag.putString("VsiaDisplayLayout",validated);tag.putString("VsiaDisplayLanguage",language);changed(stack);
    }

    private final String factoryProgramId;

    public DisplayHardDriveItem(
            Properties properties,
            String factoryProgramId
    ) {
        super(properties);
        this.factoryProgramId =
                factoryProgramId == null
                        ? ""
                        : factoryProgramId;
    }

    public String factoryProgramId() {
        return factoryProgramId;
    }

    public boolean factoryProgrammed() {
        return !factoryProgramId.isBlank();
    }

    public static boolean isDisplayDrive(
            ItemStack stack
    ) {
        return !stack.isEmpty()
                && stack.getItem()
                instanceof DisplayHardDriveItem;
    }

    public static String programId(
            ItemStack stack
    ) {
        if (!(stack.getItem()
                instanceof DisplayHardDriveItem drive)) {
            return "";
        }

        CompoundTag tag =
                stack.getTag();

        if (tag != null
                && tag.contains(TAG_PROGRAM)) {
            return tag.getString(TAG_PROGRAM);
        }

        return drive.factoryProgramId();
    }

    public static boolean hasProgram(
            ItemStack stack
    ) {
        return !programId(stack).isBlank();
    }

    public static boolean isCreateDisplay(
            ItemStack stack
    ) {
        return PROGRAM_F35_CREATE.equals(
                programId(stack)
        );
    }

    public static void setProgramId(
            ItemStack stack,
            String programId
    ) {
        if (!writable(stack)) {
            return;
        }

        stack.getOrCreateTag()
                .putString(
                        TAG_PROGRAM,
                        programId == null
                                ? ""
                                : programId
                );
        changed(stack);
    }

    public static void clearProgram(
            ItemStack stack
    ) {
        if (!writable(stack)) {
            return;
        }

        CompoundTag tag =
                stack.getOrCreateTag();

        tag.putString(
                TAG_PROGRAM,
                ""
        );

        tag.remove(
                TAG_SOURCE
        );

        tag.remove(
                TAG_NAME
        );
        tag.remove("VsiaDisplayLayout");tag.remove("VsiaDisplayLanguage");changed(stack);
    }

    public static String source(
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getTag();

        if (tag == null) {
            return "";
        }

        return tag.getString(
                TAG_SOURCE
        );
    }

    public static void setCustomSource(
            ItemStack stack,
            String name,
            String source
    ) {
        if (!writable(stack)) {
            return;
        }

        CompoundTag tag =
                stack.getOrCreateTag();

        tag.putString(
                TAG_PROGRAM,
                PROGRAM_CUSTOM
        );

        tag.putString(
                TAG_NAME,
                name == null
                        ? "Custom Display"
                        : name
        );

        tag.putString(
                TAG_SOURCE,
                source == null
                        ? ""
                        : source
        );
    }

    public static String programName(
            ItemStack stack
    ) {
        CompoundTag tag =
                stack.getTag();

        if (tag != null
                && tag.contains(TAG_NAME)) {
            return tag.getString(TAG_NAME);
        }

        String id =
                programId(stack);

        if (PROGRAM_F35_CREATE.equals(id)) {
            return "F-35 Create Display";
        }

        if (PROGRAM_CUSTOM.equals(id)) {
            return "Custom Display";
        }

        if (id.isBlank()) {
            return "Empty";
        }

        return id;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.literal(
                        "Program: "
                                + programName(stack)
                )
        );

        super.appendHoverText(
                stack,
                level,
                tooltip,
                flag
        );
    }
}
