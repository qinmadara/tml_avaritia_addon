package com.qinmadara.tml_avaritia_addon;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.qinmadara.tml_avaritia_addon.task.TaskAvaritia;
import com.qinmadara.tml_avaritia_addon.task.TaskAvaritiaBow;
import com.qinmadara.tml_avaritia_addon.task.TaskAvaritiaCrossBow;
import com.qinmadara.tml_avaritia_addon.task.TaskAvaritiaTrident;

/**
 * TLM 官方扩展入口（对应参考模组 LittleMaidImpl）。
 * 由 TLM 的扩展扫描器自动实例化（@LittleMaidExtension），无需手动注册。
 * 注册女仆战斗任务：近战（无尽剑等）+ 远程（无尽弓、无尽弩、无尽三叉戟）。
 */
@SuppressWarnings("unused")
@LittleMaidExtension
public class AvaritiaMaidExtension implements ILittleMaid {

    @Override
    public void addMaidTask(TaskManager manager) {
        manager.add(new TaskAvaritia());
        manager.add(new TaskAvaritiaBow());
        manager.add(new TaskAvaritiaCrossBow());
        manager.add(new TaskAvaritiaTrident());
    }
}
