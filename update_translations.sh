#!/bin/bash

# Script to update Kestra brand references to DataFlare in translation files
# This script updates all JSON translation files in ui/src/translations/

echo "Starting translation files update..."

# Define the translation files directory
TRANS_DIR="ui/src/translations"

# Array of translation files to update
declare -a files=(
    "de.json"
    "es.json" 
    "fr.json"
    "hi.json"
    "it.json"
    "ja.json"
    "ko.json"
    "pl.json"
    "pt.json"
    "ru.json"
    "zh_CN.json"
)

# Function to update a single file
update_file() {
    local file="$1"
    local filepath="$TRANS_DIR/$file"
    
    if [ -f "$filepath" ]; then
        echo "Updating $file..."
        
        # Create backup
        cp "$filepath" "$filepath.backup"
        
        # Update kestra.io links to dataflare.io
        sed -i 's/kestra\.io/dataflare.io/g' "$filepath"
        
        # Update Kestra brand references to DataFlare
        sed -i 's/Kestra Enterprise Edition/DataFlare Enterprise Edition/g' "$filepath"
        sed -i 's/Kestra-Workflows/DataFlare-Workflows/g' "$filepath"
        sed -i 's/Kestra-Workflow/DataFlare-Workflow/g' "$filepath"
        sed -i 's/Kestra-workflows/DataFlare-workflows/g' "$filepath"
        sed -i 's/Kestra-workflow/DataFlare-workflow/g' "$filepath"
        
        # Update specific patterns for different languages
        case "$file" in
            "de.json")
                sed -i 's/Kestra-Instanz/DataFlare-Instanz/g' "$filepath"
                sed -i 's/mit Kestra/mit DataFlare/g' "$filepath"
                ;;
            "es.json")
                sed -i 's/instancia de Kestra/instancia de DataFlare/g' "$filepath"
                sed -i 's/con Kestra/con DataFlare/g' "$filepath"
                ;;
            "fr.json")
                sed -i 's/instance Kestra/instance DataFlare/g' "$filepath"
                sed -i 's/avec Kestra/avec DataFlare/g' "$filepath"
                ;;
            "it.json")
                sed -i 's/istanza di Kestra/istanza di DataFlare/g' "$filepath"
                sed -i 's/con Kestra/con DataFlare/g' "$filepath"
                ;;
            "ja.json")
                sed -i 's/Kestraへようこそ/DataFlareへようこそ/g' "$filepath"
                ;;
            "ko.json")
                sed -i 's/Kestra에 오신 것을 환영합니다/DataFlare에 오신 것을 환영합니다/g' "$filepath"
                ;;
            "pt.json")
                sed -i 's/instância do Kestra/instância do DataFlare/g' "$filepath"
                sed -i 's/com Kestra/com DataFlare/g' "$filepath"
                ;;
            "ru.json")
                sed -i 's/экземпляр Kestra/экземпляр DataFlare/g' "$filepath"
                ;;
            "zh_CN.json")
                sed -i 's/Kestra 实例/DataFlare 实例/g' "$filepath"
                ;;
        esac
        
        echo "✅ Updated $file"
    else
        echo "❌ File $filepath not found"
    fi
}

# Update all translation files
for file in "${files[@]}"; do
    update_file "$file"
done

echo ""
echo "Translation files update completed!"
echo "Backup files created with .backup extension"
echo ""
echo "Next steps:"
echo "1. Review the changes in each file"
echo "2. Test the build with: pnpm run build"
echo "3. If everything looks good, remove backup files"
