package mindustry.content.flood;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.*;
import mindustry.graphics.*;

import static mindustry.content.Bullets.*;

@SuppressWarnings("DuplicatedCode")
public class Bullets implements ContentList{
    @Override
    public void load(){
        //lightning bullets need to be initialized first.
        damageLightning = new BulletType(0.0001f, 0f){{
            lifetime = Fx.lightning.lifetime;
            hitEffect = Fx.hitLancer;
            despawnEffect = Fx.none;
            status = StatusEffects.shocked;
            statusDuration = 10f;
            hittable = false;
        }};

        //this is just a copy of the damage lightning bullet that doesn't damage air units
        damageLightningGround = damageLightning.copy();
        damageLightningGround.collidesAir = false;

        artilleryDense = new ArtilleryBulletType(3f, 20, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 80f;
            width = height = 11f;
            collidesTiles = false;
            splashDamageRadius = 25f * 0.75f;
            splashDamage = 33f;
        }};

        artilleryPlasticFrag = new BasicBulletType(2.5f, 10, "bullet"){{
            width = 10f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 15f;
            backColor = Pal.plastaniumBack;
            frontColor = Pal.plastaniumFront;
            despawnEffect = Fx.none;
            collidesAir = false;
        }};

        artilleryPlastic = new ArtilleryBulletType(3.4f, 20, "shell"){{
            hitEffect = Fx.plasticExplosion;
            knockback = 1f;
            lifetime = 80f;
            width = height = 13f;
            collidesTiles = false;
            splashDamageRadius = 35f * 0.75f;
            splashDamage = 45f;
            fragBullet = artilleryPlasticFrag;
            fragBullets = 10;
            backColor = Pal.plastaniumBack;
            frontColor = Pal.plastaniumFront;
        }};

        artilleryHoming = new ArtilleryBulletType(3f, 20, "shell"){{
            hitEffect = Fx.flakExplosion;
            knockback = 0.8f;
            lifetime = 80f;
            width = height = 11f;
            collidesTiles = false;
            splashDamageRadius = 25f * 0.75f;
            splashDamage = 33f;
            reloadMultiplier = 1.2f;
            ammoMultiplier = 3f;
            homingPower = 0.08f;
            homingRange = 50f;
        }};

        artilleryIncendiary = new ArtilleryBulletType(3f, 20, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 80f;
            width = height = 13f;
            collidesTiles = false;
            splashDamageRadius = 25f * 0.75f;
            splashDamage = 35f;
            status = StatusEffects.burning;
            statusDuration = 60f * 12f;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            makeFire = true;
            trailEffect = Fx.incendTrail;
            ammoMultiplier = 4f;
        }};

        artilleryExplosive = new ArtilleryBulletType(2f, 20, "shell"){{
            hitEffect = Fx.blastExplosion;
            knockback = 0.8f;
            lifetime = 80f;
            width = height = 14f;
            collidesTiles = false;
            ammoMultiplier = 4f;
            splashDamageRadius = 45f * 0.75f;
            splashDamage = 55f;
            backColor = Pal.missileYellowBack;
            frontColor = Pal.missileYellow;

            status = StatusEffects.blasted;
        }};

        flakGlassFrag = new BasicBulletType(3f, 5, "bullet"){{
            width = 5f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 20f;
            backColor = Pal.gray;
            frontColor = Color.white;
            despawnEffect = Fx.none;
            collidesGround = false;
        }};

        flakLead = new FlakBulletType(4.2f, 3){{
            lifetime = 60f;
            ammoMultiplier = 4f;
            shootEffect = Fx.shootSmall;
            width = 6f;
            height = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 27f * 1.5f;
            splashDamageRadius = 15f;
        }};

        flakScrap = new FlakBulletType(4f, 3){{
            lifetime = 60f;
            ammoMultiplier = 5f;
            shootEffect = Fx.shootSmall;
            reloadMultiplier = 0.5f;
            width = 6f;
            height = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 22f * 1.5f;
            splashDamageRadius = 24f;
        }};

        flakGlass = new FlakBulletType(4f, 3){{
            lifetime = 60f;
            ammoMultiplier = 5f;
            shootEffect = Fx.shootSmall;
            reloadMultiplier = 0.8f;
            width = 6f;
            height = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 25f * 1.5f;
            splashDamageRadius = 20f;
            fragBullet = flakGlassFrag;
            fragBullets = 6;
        }};

        fragGlassFrag = new BasicBulletType(3f, 5, "bullet"){{
            width = 5f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 20f;
            backColor = Pal.gray;
            frontColor = Color.white;
            despawnEffect = Fx.none;
        }};

        fragPlasticFrag = new BasicBulletType(2.5f, 10, "bullet"){{
            width = 10f;
            height = 12f;
            shrinkY = 1f;
            lifetime = 15f;
            backColor = Pal.plastaniumBack;
            frontColor = Pal.plastaniumFront;
            despawnEffect = Fx.none;
        }};

        fragGlass = new FlakBulletType(4f, 20){{
            ammoMultiplier = 6f;
            shootEffect = Fx.shootSmall;
            reloadMultiplier = 0.8f;
            width = 6f;
            height = 8f;
            hitEffect = Fx.flakExplosion;
            splashDamage = 18f * 1.5f;
            splashDamageRadius = 16f;
            fragBullet = fragGlassFrag;
            fragBullets = 4;
            explodeRange = 20f;
            collidesGround = true;
        }};

        fragPlastic = new FlakBulletType(4f, 30){{
            splashDamageRadius = 60f;
            splashDamage = 25f * 1.5f;
            fragBullet = fragPlasticFrag;
            fragBullets = 6;
            ammoMultiplier = 4f;
            hitEffect = Fx.plasticExplosion;
            frontColor = Pal.plastaniumFront;
            backColor = Pal.plastaniumBack;
            shootEffect = Fx.shootBig;
            collidesGround = true;
            explodeRange = 20f;
        }};

        fragExplosive = new FlakBulletType(4f, 20){{
            shootEffect = Fx.shootBig;
            ammoMultiplier = 7f;
            splashDamage = 40f * 1.5f;
            splashDamageRadius = 75f;
            collidesGround = true;

            status = StatusEffects.blasted;
            statusDuration = 60f;
        }};

        fragSurge = new FlakBulletType(4.5f, 35){{
            ammoMultiplier = 6f;
            splashDamage = 75f * 1.5f;
            splashDamageRadius = 38f;
            lightning = 2;
            lightningLength = 15;
            shootEffect = Fx.shootBig;
            collidesGround = true;
            explodeRange = 20f;
        }};

        missileExplosive = new MissileBulletType(3.7f, 50){{
            width = 8f;
            height = 8f;
            shrinkY = 0f;
            splashDamageRadius = 30f;
            splashDamage = 75f * 1.5f;
            ammoMultiplier = 4f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;

            status = StatusEffects.blasted;
            statusDuration = 60f;
        }};

        missileIncendiary = new MissileBulletType(3.7f, 60){{
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            width = 7f;
            height = 8f;
            shrinkY = 0f;
            homingPower = 0.08f;
            splashDamageRadius = 20f;
            splashDamage = 40f * 1.5f;
            makeFire = true;
            ammoMultiplier = 4f;
            hitEffect = Fx.blastExplosion;
            status = StatusEffects.burning;
        }};

        missileSurge = new MissileBulletType(3.7f, 60){{
            width = 8f;
            height = 8f;
            shrinkY = 0f;
            splashDamageRadius = 25f;
            splashDamage = 40f * 1.4f;
            hitEffect = Fx.blastExplosion;
            despawnEffect = Fx.blastExplosion;
            ammoMultiplier = 3f;
            lightningDamage = 40;
            lightning = 2;
            lightningLength = 20;
        }};

        standardCopper = new BasicBulletType(2.5f, 35){{
            width = 7f;
            height = 9f;
            lifetime = 60f;
            shootEffect = Fx.shootSmall;
            smokeEffect = Fx.shootSmallSmoke;
            ammoMultiplier = 5;
        }};

        standardDense = new BasicBulletType(3.5f, 50){{
            width = 9f;
            height = 12f;
            reloadMultiplier = 0.6f;
            ammoMultiplier = 6;
            lifetime = 60f;
        }};

        standardThorium = new BasicBulletType(4f, 60, "bullet"){{
            width = 10f;
            height = 13f;
            shootEffect = Fx.shootBig;
            smokeEffect = Fx.shootBigSmoke;
            ammoMultiplier = 6;
            pierceCap = 2;
            pierceBuilding = true;
            lifetime = 60f;
        }};

        standardHoming = new BasicBulletType(3f, 50, "bullet"){{
            width = 7f;
            height = 9f;
            homingPower = 0.1f;
            reloadMultiplier = 1.5f;
            ammoMultiplier = 6;
            lifetime = 60f;
        }};

        standardIncendiary = new BasicBulletType(3.2f, 55, "bullet"){{
            width = 10f;
            height = 12f;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            status = StatusEffects.burning;
            hitEffect = new MultiEffect(Fx.hitBulletSmall, Fx.fireHit);

            ammoMultiplier = 6;

            splashDamage = 25f;
            splashDamageRadius = 22f;

            makeFire = true;
            lifetime = 60f;
        }};

        standardDenseBig = new BasicBulletType(7.5f, 50, "bullet"){{
            hitSize = 4.8f;
            width = 15f;
            height = 21f;
            pierceCap = 6;
            pierceBuilding = true;
            shootEffect = Fx.shootBig;
            ammoMultiplier = 4;
            reloadMultiplier = 1.7f;
            knockback = 0.3f;
        }};

        standardThoriumBig = new BasicBulletType(8f, 50, "bullet"){{
            hitSize = 5;
            width = 16f;
            height = 23f;
            shootEffect = Fx.shootBig;
            pierceCap = 8;
            pierceBuilding = true;
            knockback = 0.7f;
        }};

        standardIncendiaryBig = new BasicBulletType(7f, 1, "bullet"){{
            width = 16f;
            height = 21f;
            frontColor = Pal.lightishOrange;
            backColor = Pal.lightOrange;
            status = StatusEffects.burning;
            hitEffect = new MultiEffect(Fx.hitBulletSmall, Fx.fireHit);
            shootEffect = Fx.shootBig;
            makeFire = true;
            pierceCap = 16;
            pierceBuilding = true;
            knockback = 0.6f;
            ammoMultiplier = 3;
            splashDamage = 15f;
            splashDamageRadius = 24f;
        }};

        fireball = new FireBulletType(1f, 4);

        basicFlame = new BulletType(3.35f, 40f){{
            ammoMultiplier = 3f;
            hitSize = 7f;
            lifetime = 18f;
            pierce = true;
            collidesAir = false;
            statusDuration = 60f * 4;
            shootEffect = Fx.shootSmallFlame;
            hitEffect = Fx.hitFlameSmall;
            despawnEffect = Fx.none;
            status = StatusEffects.burning;
            keepVelocity = false;
            hittable = false;
        }};

        pyraFlame = new BulletType(4f, 100f){{
            ammoMultiplier = 6f;
            hitSize = 7f;
            lifetime = 18f;
            pierce = true;
            collidesAir = false;
            statusDuration = 60f * 10;
            shootEffect = Fx.shootPyraFlame;
            hitEffect = Fx.hitFlameSmall;
            despawnEffect = Fx.none;
            status = StatusEffects.burning;
            hittable = false;
        }};

        waterShot = new LiquidBulletType(Liquids.water){{
            knockback = 0.7f;
            drag = 0.01f;
        }};

        cryoShot = new LiquidBulletType(Liquids.cryofluid){{
            drag = 0.01f;
        }};

        slagShot = new LiquidBulletType(Liquids.slag){{
            damage = 8;
            drag = 0.01f;
        }};

        oilShot = new LiquidBulletType(Liquids.oil){{
            damage = 3;
            drag = 0.01f;
        }};

        heavyWaterShot = new LiquidBulletType(Liquids.water){{
            lifetime = 49f;
            speed = 4f;
            knockback = 1.7f;
            puddleSize = 8f;
            orbSize = 4f;
            drag = 0.001f;
            ammoMultiplier = 0.4f;
            statusDuration = 60f * 4f;
            damage = 1f;
        }};

        heavyCryoShot = new LiquidBulletType(Liquids.cryofluid){{
            lifetime = 49f;
            speed = 4f;
            knockback = 1.3f;
            puddleSize = 8f;
            orbSize = 4f;
            drag = 0.001f;
            ammoMultiplier = 0.4f;
            statusDuration = 60f * 4f;
            damage = 3f;
        }};

        heavySlagShot = new LiquidBulletType(Liquids.slag){{
            lifetime = 49f;
            speed = 4f;
            knockback = 1.3f;
            puddleSize = 16f;
            orbSize = 4f;
            damage = 12f;
            drag = 0.001f;
            ammoMultiplier = 0.4f;
            statusDuration = 60f * 4f;
        }};

        heavyOilShot = new LiquidBulletType(Liquids.oil){{
            lifetime = 49f;
            speed = 4f;
            knockback = 1.3f;
            puddleSize = 16f;
            orbSize = 4f;
            drag = 0.001f;
            ammoMultiplier = 0.4f;
            statusDuration = 60f * 4f;
            damage = 4.75f;
        }};
    }
}
