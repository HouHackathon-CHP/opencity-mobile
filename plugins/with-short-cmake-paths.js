// Windows Ninja can reject React Native codegen paths over MAX_PATH.
// CMake hashes object file paths when this limit is set explicitly.
const { withAppBuildGradle } = require('expo/config-plugins');

module.exports = function withShortCmakePaths(config) {
  return withAppBuildGradle(config, (mod) => {
    if (mod.modResults.language !== 'groovy') return mod;
    if (mod.modResults.contents.includes('CMAKE_OBJECT_PATH_MAX')) {
      mod.modResults.contents = mod.modResults.contents.replace(/CMAKE_OBJECT_PATH_MAX=\d+/, 'CMAKE_OBJECT_PATH_MAX=220');
    } else {
      mod.modResults.contents = mod.modResults.contents.replace(
        /(defaultConfig\s*\{)/,
        '$1\n        externalNativeBuild { cmake { arguments "-DCMAKE_OBJECT_PATH_MAX=220" } }',
      );
    }
    return mod;
  });
};
