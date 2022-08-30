

material-1.3.0\res\values\values.xml
```
<style name="Theme.MaterialComponents" parent="Base.Theme.MaterialComponents"/>

<style name="Base.Theme.MaterialComponents" parent="Base.V14.Theme.MaterialComponents"/>

<style name="Base.V14.Theme.MaterialComponents" parent="Base.V14.Theme.MaterialComponents.Bridge">

<style name="Base.V14.Theme.MaterialComponents.Bridge" parent="Platform.MaterialComponents">
    <item name="isMaterialTheme">true</item>
    <item name="colorPrimaryVariant">@color/design_dark_default_color_primary_variant</item>
    <item name="colorSecondary">@color/design_dark_default_color_secondary</item>
    <item name="colorSecondaryVariant">@color/design_dark_default_color_secondary_variant</item>
    ....   

<style name="Platform.MaterialComponents" parent="Theme.AppCompat"/>    
```