export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" # 这将加载 nvm


#
SCRIPT_DIR=$(dirname "$(realpath "$0")")
PARENT_DIR=$(dirname "$SCRIPT_DIR")

# # 编译 ambari-web
echo "Building ambari-web..."
cd "$PARENT_DIR" || exit 1
npm run build