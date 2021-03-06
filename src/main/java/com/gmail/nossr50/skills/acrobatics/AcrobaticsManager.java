package com.gmail.nossr50.skills.acrobatics;

import com.gmail.nossr50.config.experience.ExperienceConfig;
import com.gmail.nossr50.datatypes.LimitedSizeList;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.interactions.NotificationType;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.skills.SkillManager;
import com.gmail.nossr50.util.Misc;
import com.gmail.nossr50.util.Permissions;
import com.gmail.nossr50.util.player.NotificationManager;
import com.gmail.nossr50.util.random.RandomChanceUtil;
import com.gmail.nossr50.util.skills.ParticleEffectUtils;
import com.gmail.nossr50.util.skills.RankUtils;
import com.gmail.nossr50.util.skills.SkillActivationType;
import com.gmail.nossr50.util.skills.SkillUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;

public class AcrobaticsManager extends SkillManager {

    public AcrobaticsManager(McMMOPlayer mcMMOPlayer) {
        super(mcMMOPlayer, PrimarySkillType.ACROBATICS);
        fallLocationMap = new LimitedSizeList(50);
    }

    private long rollXPCooldown = 0;
    private long rollXPInterval = (1000 * 3); //1 Minute
    private long rollXPIntervalLengthen = (1000 * 10); //10 Seconds
    private LimitedSizeList fallLocationMap;

    public boolean hasFallenInLocationBefore(Location location)
    {
        return fallLocationMap.contains(location);
    }

    public void addLocationToFallMap(Location location)
    {
        fallLocationMap.add(location);
    }

    public boolean canGainRollXP()
    {
        if(!ExperienceConfig.getInstance().isAcrobaticsExploitingPrevented())
            return true;

        if(System.currentTimeMillis() >= rollXPCooldown)
        {
            rollXPCooldown = System.currentTimeMillis() + rollXPInterval;
            rollXPIntervalLengthen = (1000 * 10); //5 Seconds
            return true;
        } else {
            rollXPCooldown += rollXPIntervalLengthen;
            rollXPIntervalLengthen += 1000; //Add another second to the next penalty
            return false;
        }
    }

    public boolean canDodge(Entity damager) {
        if(!RankUtils.hasUnlockedSubskill(getPlayer(), SubSkillType.ACROBATICS_DODGE))
            return false;

        if (Permissions.isSubSkillEnabled(getPlayer(), SubSkillType.ACROBATICS_DODGE)) {
            if (damager instanceof LightningStrike && Acrobatics.dodgeLightningDisabled) {
                return false;
            }

            return skill.shouldProcess(damager);
        }

        return false;
    }

    /**
     * Handle the damage reduction and XP gain from the Dodge ability
     *
     * @param damage The amount of damage initially dealt by the event
     * @return the modified event damage if the ability was successful, the original event damage otherwise
     */
    public double dodgeCheck(double damage) {
        double modifiedDamage = Acrobatics.calculateModifiedDodgeDamage(damage, Acrobatics.dodgeDamageModifier);
        Player player = getPlayer();

        if (!isFatal(modifiedDamage) && RandomChanceUtil.isActivationSuccessful(SkillActivationType.RANDOM_LINEAR_100_SCALE_WITH_CAP, SubSkillType.ACROBATICS_DODGE, player)) {
            ParticleEffectUtils.playDodgeEffect(player);

            if (mcMMOPlayer.useChatNotifications()) {
                NotificationManager.sendPlayerInformation(player, NotificationType.SUBSKILL_MESSAGE, "Acrobatics.Combat.Proc");
            }

            //Check respawn to prevent abuse
            if (SkillUtils.cooldownExpired(mcMMOPlayer.getRespawnATS(), Misc.PLAYER_RESPAWN_COOLDOWN_SECONDS)) {
                applyXpGain((float) (damage * Acrobatics.dodgeXpModifier), XPGainReason.PVP);
            }

            return modifiedDamage;
        }

        return damage;
    }

    private boolean isFatal(double damage) {
        return getPlayer().getHealth() - damage <= 0;
    }
}
