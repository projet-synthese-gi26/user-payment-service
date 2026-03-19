const fs = require('fs');
const path = require('path');

/**
 * Générateur de contexte de projet pour IA
 * À placer à la racine du projet Spring Boot
 * 
 * Exécution: node generate-project-context.js
 */

const CONFIG = {
    outputFile: 'project-context-for-ai.md',
    excludedDirs: new Set(['target', '.idea', '.mvn', 'node_modules', '.git', 'build', 'bin']),
    includedExtensions: new Set(['.java', '.xml', '.properties', '.yml', '.yaml', '.json', '.sql', '.md', '.txt']),
    maxFileSize: 500000, // 500KB max par fichier
};

class ProjectContextGenerator {
    constructor() {
        this.stats = {
            totalFiles: 0,
            totalLines: 0,
            filesByType: {}
        };
    }

    generate() {
        console.log('🚀 Génération du contexte du projet...');
        
        const projectRoot = process.cwd();
        let context = '';
        
        // En-tête
        context += this.generateHeader(projectRoot);
        
        // Structure du projet
        context += this.generateProjectStructure(projectRoot);
        
        // Contenu des fichiers
        context += this.generateFileContents(projectRoot);
        
        // Statistiques
        context += this.generateStatistics();
        
        // Écriture du fichier
        fs.writeFileSync(CONFIG.outputFile, context, 'utf8');
        
        console.log(`✅ Contexte généré avec succès: ${CONFIG.outputFile}`);
        const fileSize = fs.statSync(CONFIG.outputFile).size;
        console.log(`📊 Taille: ${(fileSize / 1024).toFixed(2)} KB`);
    }

    generateHeader(projectRoot) {
        const projectName = path.basename(projectRoot);
        const date = new Date().toLocaleString('fr-FR');
        
        return `# Contexte Complet du Projet

**Projet:** ${projectName}  
**Date de génération:** ${date}  
**Chemin:** ${projectRoot}

---

## Table des matières
1. [Structure du projet](#structure-du-projet)
2. [Contenu des fichiers](#contenu-des-fichiers)
3. [Statistiques](#statistiques)

---

`;
    }

    generateProjectStructure(rootPath) {
        let output = '## Structure du projet\n\n```\n';
        output += this.buildTree(rootPath, '', true);
        output += '```\n\n---\n\n';
        return output;
    }

    buildTree(dir, prefix = '', isRoot = false) {
        let result = '';
        
        try {
            const items = fs.readdirSync(dir, { withFileTypes: true })
                .filter(item => !CONFIG.excludedDirs.has(item.name))
                .sort((a, b) => {
                    if (a.isDirectory() === b.isDirectory()) {
                        return a.name.localeCompare(b.name);
                    }
                    return a.isDirectory() ? -1 : 1;
                });

            items.forEach((item, index) => {
                const isLast = index === items.length - 1;
                const connector = isLast ? '└── ' : '├── ';
                const newPrefix = prefix + (isLast ? '    ' : '│   ');
                
                result += `${prefix}${connector}${item.name}\n`;
                
                if (item.isDirectory()) {
                    const fullPath = path.join(dir, item.name);
                    result += this.buildTree(fullPath, newPrefix, false);
                }
            });
        } catch (error) {
            console.error(`Erreur lors de la lecture de ${dir}:`, error.message);
        }
        
        return result;
    }

    generateFileContents(rootPath) {
        let output = '## Contenu des fichiers\n\n';
        const files = this.getAllFiles(rootPath);
        
        files.forEach(file => {
            output += this.generateFileSection(file, rootPath);
        });
        
        return output;
    }

    getAllFiles(dir, fileList = []) {
        try {
            const items = fs.readdirSync(dir, { withFileTypes: true });
            
            items.forEach(item => {
                const fullPath = path.join(dir, item.name);
                
                if (item.isDirectory()) {
                    if (!CONFIG.excludedDirs.has(item.name)) {
                        this.getAllFiles(fullPath, fileList);
                    }
                } else {
                    const ext = path.extname(item.name);
                    if (CONFIG.includedExtensions.has(ext)) {
                        const stats = fs.statSync(fullPath);
                        if (stats.size <= CONFIG.maxFileSize) {
                            fileList.push(fullPath);
                        }
                    }
                }
            });
        } catch (error) {
            console.error(`Erreur lors de la lecture de ${dir}:`, error.message);
        }
        
        return fileList;
    }

    generateFileSection(filePath, rootPath) {
        const relativePath = path.relative(rootPath, filePath);
        const ext = path.extname(filePath).substring(1);
        
        let output = `### 📄 ${relativePath}\n\n`;
        
        try {
            const content = fs.readFileSync(filePath, 'utf8');
            const lines = content.split('\n').length;
            
            // Mise à jour des statistiques
            this.stats.totalFiles++;
            this.stats.totalLines += lines;
            this.stats.filesByType[ext] = (this.stats.filesByType[ext] || 0) + 1;
            
            // Détection du langage pour la coloration syntaxique
            const language = this.getLanguage(ext);
            
            output += `\`\`\`${language}\n`;
            output += content;
            output += '\n```\n\n';
            output += `*Lignes: ${lines}*\n\n---\n\n`;
            
        } catch (error) {
            output += `*⚠️ Impossible de lire le fichier: ${error.message}*\n\n---\n\n`;
        }
        
        return output;
    }

    getLanguage(extension) {
        const languageMap = {
            'java': 'java',
            'xml': 'xml',
            'properties': 'properties',
            'yml': 'yaml',
            'yaml': 'yaml',
            'json': 'json',
            'sql': 'sql',
            'md': 'markdown',
            'txt': 'text'
        };
        return languageMap[extension] || extension;
    }

    generateStatistics() {
        let output = '## Statistiques\n\n';
        output += `- **Total de fichiers analysés:** ${this.stats.totalFiles}\n`;
        output += `- **Total de lignes de code:** ${this.stats.totalLines.toLocaleString('fr-FR')}\n`;
        output += `- **Moyenne de lignes par fichier:** ${Math.round(this.stats.totalLines / this.stats.totalFiles)}\n\n`;
        
        output += '### Répartition par type de fichier\n\n';
        const sortedTypes = Object.entries(this.stats.filesByType)
            .sort((a, b) => b[1] - a[1]);
        
        sortedTypes.forEach(([type, count]) => {
            output += `- **.${type}:** ${count} fichier${count > 1 ? 's' : ''}\n`;
        });
        
        output += '\n---\n\n';
        output += '*Contexte généré automatiquement pour analyse par IA*\n';
        
        return output;
    }
}

// Exécution
try {
    const generator = new ProjectContextGenerator();
    generator.generate();
} catch (error) {
    console.error('❌ Erreur fatale:', error);
    process.exit(1);
}