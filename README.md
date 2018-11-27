
# react-native-jpush-module

## Getting started

`$ npm install react-native-jpush-module --save`

### Mostly automatic installation

`$ react-native link react-native-jpush-module`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-jpush-module` and add `TMJpushModule.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libTMJpushModule.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.as.jpush.TMJPushPackage;` to the imports at the top of the file
  - Add `new TMJPushPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-jpush-module'
  	project(':react-native-jpush-module').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-jpush-module/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-jpush-module')
  	```


## Usage
```javascript
import JPushModule from 'react-native-jpush-module';

// TODO: What to do with the module?
JPushModule;
```
  
