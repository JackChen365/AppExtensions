#!/usr/bin/env sh

# Reset the install file
git reset app_extension_install.sh
git reset app_extension_init.gradle

# Add the app_extension to global ignore file.
global_git_ignore="*_extension/
.extensions
"
rm ~/.global_git_ignore.txt
echo "$global_git_ignore" > ~/.global_git_ignore.txt
git config --global core.excludesfile ~/.global_git_ignore.txt

# Put the init.gradle to USER_HOME/.gradle/init.d/
if [ ! -f ~/.gradle/init.d/ ]; then
  mkdir ~/.gradle/init.d/
fi
cp app_extension_init.gradle ~/.gradle/init.d/