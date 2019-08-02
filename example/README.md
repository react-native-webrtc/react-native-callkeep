# CallKeep example



## How to install it

```
# Install dependancies
yarn install

cd ios
pod install
```

## How to use it

```
# Start metro bundler
yarn start

# Start the application (in another term)
yarn android # or yarn ios
```


## How this example was setted up

```sh
expo init CallKeepDemo
expo eject
yarn add react-native-callkeep
./node_modules/.bin/react-native link react-native-callkeep
```
