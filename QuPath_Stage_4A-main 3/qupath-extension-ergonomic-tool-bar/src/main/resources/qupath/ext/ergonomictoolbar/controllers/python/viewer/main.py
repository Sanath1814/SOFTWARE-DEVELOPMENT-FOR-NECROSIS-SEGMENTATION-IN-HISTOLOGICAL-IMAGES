"""
Viewer Standalone - Version consolidée de l'application de visualisation d'images.
Ce fichier contient tout le code nécessaire pour l'application, regroupé en un seul fichier
pour éviter les problèmes d'importation lors de la compilation en exécutable.
"""
import os
import sys
import time
from typing import Dict, List, Optional, Tuple, Any
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.widgets import Button
from matplotlib.axes import Axes
from matplotlib.figure import Figure
from PIL import Image
from tkinter import messagebox, Tk


#############################################
# Module utils - Fonctions utilitaires
#############################################

def afficher_boite_dialogue(titre: str, message: str) -> None:
    """
    Affiche une boîte de dialogue avec un titre et un message.

    Args :
        titre (str): Le titre de la boîte de dialogue.
        message (str): Le message à afficher dans la boîte de dialogue.
    """
    root = Tk()
    root.withdraw()
    messagebox.showinfo(titre, message)


def create_directory(path: str) -> None:
    """
    Crée un répertoire s'il n'existe pas déjà.

    Args :
        path (str): Le chemin du répertoire à créer.
    """
    os.makedirs(path, exist_ok=True)


def get_image_files(dossier: str) -> List[str]:
    """
    Récupère la liste des fichiers d'images dans un dossier.

    Args :
        dossier (str): Le chemin du dossier contenant les images.

    Returns :
        List[str]: Liste des fichiers d'images.
    """
    return [f for f in os.listdir(dossier) if f.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp', '.gif', 'tif', 'tiff'))]


#############################################
# Module file_manager - Gestion des fichiers
#############################################

class FileManager:
    """
    Classe responsable de la gestion des fichiers et dossiers pour l'application.
    """

    def __init__(self, model_path: str) -> None:
        """
        Initialise le gestionnaire de fichiers.

        Args :
            model_path (str): Chemin vers le dossier du modèle.
        """
        self.model_path: str = model_path
        self.viable_path: str = os.path.join(model_path, "Viable")
        self.necrotic_path: str = os.path.join(model_path, "Necrotic")
        self.other_path: str = os.path.join(model_path, "Other")
        self.doubtful_path: str = os.path.join(model_path, "Doubtful")

        # Créer les dossiers de destination s'ils n'existent pas
        self._create_destination_folders()

        # Initialiser le dossier tumor_area
        self._initialiser_dossier_tumor_area()

    def _create_destination_folders(self) -> None:
        """Crée les dossiers de destination s'ils n'existent pas."""
        directories: List[str] = ["Viable", "Necrotic", "Other", "Doubtful"]
        for directory in directories:
            os.makedirs(os.path.join(self.model_path, directory), exist_ok=True)
        print(f"Les dossiers ont été créés avec succès dans {self.model_path}")

    def _initialiser_dossier_tumor_area(self) -> None:
        """Initialise le dossier 'tumor_area' avec l'image la plus grande."""
        # Chemin du dossier "datasets"
        chemin_datasets: str = os.path.join(os.path.dirname(os.path.realpath('__file__')), "datasets")
        # Chemin du dossier "tumor_area"
        chemin_tumor_area: str = os.path.join(chemin_datasets, "tumor_area")

        # Vérifier si le dossier "tumor_area" existe déjà et est rempli
        if os.path.exists(chemin_tumor_area) and os.listdir(chemin_tumor_area):
            print("Le dossier 'tumor_area' existe déjà et est rempli. Rien à faire.")
            return

        # Créer le dossier "tumor_area" s'il n'existe pas
        os.makedirs(chemin_tumor_area, exist_ok=True)

        # Trouver l'image la plus grande dans "datasets"
        chemin_image_max: Optional[str] = self.trouver_image_plus_grande(chemin_datasets)

        if chemin_image_max:
            # Copier l'image dans le dossier "tumor_area"
            nom_image: str = os.path.basename(chemin_image_max)
            destination: str = os.path.join(chemin_tumor_area, nom_image)
            import shutil
            shutil.copy(chemin_image_max, destination)
            os.remove(chemin_image_max)

            print(f"L'image {nom_image} a été déplacée dans le dossier 'tumor_area'.")
        else:
            print("Aucune image valide trouvée dans le dossier 'datasets'.")

    @staticmethod
    def trouver_image_plus_grande(chemin_dossier: str) -> Optional[str]:
        """
        Trouve l'image avec les plus grandes dimensions dans le dossier donné.

        Args :
            chemin_dossier (str): Chemin du dossier à parcourir.

        Returns :
            Optional[str]: Chemin de l'image la plus grande, ou None si aucune image n'est trouvée.
        """
        image_la_plus_grande: Optional[str] = None
        dimensions_max: int = 0

        # Parcourir tous les fichiers et sous-dossiers
        for racine, _, fichiers in os.walk(chemin_dossier):
            for fichier in fichiers:
                chemin_complet: str = os.path.join(str(racine), fichier)
                try:
                    # Ouvrir l'image et vérifier ses dimensions
                    with Image.open(chemin_complet) as img:
                        largeur, hauteur = img.size
                        dimensions: int = largeur * hauteur
                        if dimensions > dimensions_max:
                            dimensions_max = dimensions
                            image_la_plus_grande = chemin_complet
                except Exception as e:
                    # Ignorer les fichiers qui ne sont pas des images
                    print(f"Impossible d'ouvrir {chemin_complet}: {e}")
                    continue

        return image_la_plus_grande

    @staticmethod
    def vider_dossier(chemin_dossier: str) -> None:
        """
        Supprime tout le contenu d'un dossier.

        Args :
            chemin_dossier (str): Chemin du dossier à vider.
        """
        import shutil
        for root, dirs, files in os.walk(chemin_dossier):
            for fichier in files:
                try:
                    os.remove(os.path.join(root, fichier))
                except Exception as e:
                    print(f"Erreur lors de la suppression du fichier {fichier}: {e}")
            for dossier in dirs:
                try:
                    shutil.rmtree(os.path.join(root, dossier))
                except Exception as e:
                    print(f"Erreur lors de la suppression du dossier {dossier}: {e}")

    def deplacer_image(self, image_chemin: str, categorie: str) -> None:
        """
        Déplace une image vers le dossier correspondant à sa catégorie.

        Args :
            image_chemin (str): Chemin de l'image à déplacer.
            categorie (str): Catégorie de l'image ('red', 'black', 'yellow', 'purple').
        """
        import shutil
        destination: Optional[str] = None
        if categorie == 'red':
            destination = self.viable_path
        elif categorie == 'black':
            destination = self.necrotic_path
        elif categorie == 'yellow':
            destination = self.other_path
        elif categorie == 'purple':
            destination = self.doubtful_path

        if destination:
            try:
                shutil.copy(image_chemin, destination)
            except FileNotFoundError:
                return
            os.remove(image_chemin)


#############################################
# Module image_processor - Traitement d'images
#############################################

class ImageProcessor:
    """
    Classe responsable du traitement et de la manipulation des images.
    """

    def __init__(self) -> None:
        """Initialise le processeur d'images."""
        # Définition des couleurs
        self.colors: Dict[str, Tuple[int, int, int]] = {
            'red': (255, 0, 0),
            'black': (0, 0, 0),
            'yellow': (255, 255, 0),
            'purple': (163, 73, 163)
        }

    def appliquer_filtre(self, image: Image.Image, couleur_nom: str) -> Image.Image:
        """
        Applique un filtre de couleur sur l'image.

        Args :
            image (PIL.Image): L'image à filtrer.
            couleur_nom (str): Le nom de la couleur à appliquer ('red', 'black', 'yellow', 'purple').

        Returns :
            PIL.Image: L'image avec le filtre appliqué.
        """
        if couleur_nom not in self.colors:
            return image

        image = image.convert('RGB')
        overlay = Image.new('RGB', image.size, self.colors[couleur_nom])
        return Image.blend(image, overlay, alpha=0.3)

    def pre_generer_filtres(self, image: Image.Image) -> Dict[str, Image.Image]:
        """
        Pré-génère les filtres pour une image donnée.

        Args :
            image (PIL.Image): L'image originale.

        Returns :
            Dict[str, Image.Image]: Dictionnaire contenant les versions filtrées de l'image.
        """
        filters: Dict[str, Image.Image] = {}
        for color_name, color_value in self.colors.items():
            filters[color_name] = self.appliquer_filtre(image, color_name)
        return filters

    @staticmethod
    def determiner_categorie_par_nom_fichier(filepath: str) -> str:
        """
        Détermine la catégorie d'une image en fonction de son nom de fichier.

        Args :
            filepath (str): Chemin du fichier image.

        Returns :
            str: Nom de la catégorie ('red', 'black', 'yellow', 'purple') ou chaîne vide si non déterminé.
        """
        filepath_lower = filepath.lower()
        if "necrosis" in filepath_lower:
            return "black"
        elif "other" in filepath_lower:
            return "yellow"
        elif "tumor" in filepath_lower:
            return "red"
        else:
            return "purple"


#############################################
# Module image_viewer - Interface utilisateur
#############################################

class ImageViewer:
    """
    Classe principale pour l'affichage et l'interaction avec les images.
    """

    def __init__(self, dossier: str, file_manager: FileManager, image_processor: ImageProcessor, taille_grille: int = 6) -> None:
        """
        Initialise le visualiseur d'images.

        Args :
            dossier (str): Chemin du dossier contenant les images à afficher.
            file_manager (FileManager): Gestionnaire de fichiers.
            image_processor (ImageProcessor): Processeur d'images.
            taille_grille (int, optional): Taille de la grille d'images. Par défaut à 6.
        """
        self.dossier: str = dossier
        self.file_manager: FileManager = file_manager
        self.image_processor: ImageProcessor = image_processor
        self.taille_grille: int = taille_grille

        # État des boutons
        self.button_states: Dict[str, bool] = {
            'r': False,  # Viable (rouge)
            'n': False,  # Necrotic (noir)
            'j': False,  # Other (jaune)
            'doute': False,  # Doubtful (violet)
            'zoom': False,  # Zoom
            'carte': False   # Carte
        }

        # Variables d'état
        self.images_fichiers_restants: List[str] = []
        self.last_update_time: float = time.time()
        self.mouse_pressed: bool = False
        self.current_ax: Optional[Axes] = None

        # État de l'image agrandie
        self.image_grande_affichee: bool = False
        self.image_grande_ax: Optional[Axes] = None

        # Dictionnaire pour stocker les axes et les images
        self.image_axes: Dict[Axes, Dict[str, Any]] = {}

        # Éléments d'interface
        self.fig: Optional[Figure] = None
        self.axes: Optional[Any] = None
        self.bouton_r: Optional[Button] = None
        self.bouton_n: Optional[Button] = None
        self.bouton_j: Optional[Button] = None
        self.bouton_doute: Optional[Button] = None
        self.bouton_carte: Optional[Button] = None
        self.bouton_zoom: Optional[Button] = None
        self.bouton_terminer: Optional[Button] = None

        # Configuration de matplotlib
        plt.rcParams['toolbar'] = 'none'

        # L'interface sera initialisée lors de l'appel à afficher()

    def _initialiser_interface(self) -> bool:
        """
        Initialise l'interface graphique.

        Returns :
            bool: True si l'initialisation a réussi, False sinon.
        """
        # Initialiser la liste des fichiers d'image restants si vide
        if not self.images_fichiers_restants:
            self.images_fichiers_restants = [
                f for f in os.listdir(self.dossier)
                if f.lower().endswith(('.png', '.jpg', '.jpeg', '.bmp', '.gif', 'tif', 'tiff'))
            ]

        # Charger les 36 premières images disponibles
        images_fichiers: List[str] = self.images_fichiers_restants[:36]
        self.images_fichiers_restants = self.images_fichiers_restants[36:]

        # Vérifier si toutes les images ont été traitées
        if not images_fichiers:
            file_dir: str = os.path.dirname(os.path.realpath('__file__'))
            chemin_datasets: str = os.path.join(file_dir, "datasets")

            # Vider le dossier "datasets"
            if os.path.exists(chemin_datasets):
                self.file_manager.vider_dossier(chemin_datasets)

            afficher_boite_dialogue(
                "Export terminé",
                "Les images ont été classées dans les dossiers correspondants :\n"
                "-Necrotic -Viable -Other -Doubtful\n"
                f"Au chemin suivant :\n{self.file_manager.model_path}"
            )
            return False  # Rien à afficher

        # Initialiser la figure pour la grille d'images
        self.fig, self.axes = plt.subplots(
            self.taille_grille, self.taille_grille, figsize=(150, 150), dpi=110
        )

        # Ajuster automatiquement l'affichage
        self.fig.tight_layout()
        manager = plt.get_current_fig_manager()
        manager.resize(640, 480)

        # Affiche chaque image dans la grille
        for idx, image_fichier in enumerate(images_fichiers):
            image_chemin: str = os.path.join(self.dossier, image_fichier)
            image: Image.Image = Image.open(image_chemin)

            # Pré-générer les filtres pour accélérer le survol
            filters: Dict[str, Image.Image] = self.image_processor.pre_generer_filtres(image)

            row, col = divmod(idx, self.taille_grille)
            ax: Axes = self.axes[row, col]

            # Stocke l'axe et les données de l'image
            self.image_axes[ax] = {
                'original_image': image,
                'displayed_image': None,
                'filtered_images': filters,
                'patch': None,
                'contour_color': None,
                'filepath': image_chemin
            }

            # Déterminer la couleur en fonction du nom de fichier
            color: str = self.image_processor.determiner_categorie_par_nom_fichier(
                self.image_axes[ax]["filepath"]
            )

            if color:
                self.image_axes[ax]['displayed_image'] = self.image_axes[ax]["filtered_images"][color]
                self.image_axes[ax]['contour_color'] = color
            else:
                self.image_axes[ax]['displayed_image'] = image

            ax.imshow(self.image_axes[ax]['displayed_image'])
            self._appliquer_contour(ax, self.image_axes[ax], color)
            ax.axis('off')

        # Masque les axes vides
        for i in range(len(images_fichiers), self.taille_grille * self.taille_grille):
            row, col = divmod(i, self.taille_grille)
            self.axes[row, col].axis('off')

        # Créer les boutons
        self._creer_boutons()

        # Connecter les événements
        self._connect_events()

        # Ajuster la disposition
        self.fig.subplots_adjust(right=0.8)
        self.fig.canvas.manager.set_window_title("Visualiseur et classifieur d'images")

        return True  # Interface initialisée avec succès

    @staticmethod
    def _appliquer_contour(ax: Axes, data: Dict[str, Any], couleur: str) -> None:
        """
        Applique un contour coloré autour d'une image.

        Args :
            ax (matplotlib.axes.Axes): L'axe contenant l'image.
            data (Dict[str, Any]): Données associées à l'image.
            couleur (str): Couleur du contour.
        """
        patch = patches.Rectangle(
            (0, 0),
            ax.get_xlim()[1],
            ax.get_ylim()[0],
            linewidth=5,
            edgecolor=couleur,
            facecolor='none'
        )
        ax.add_patch(patch)
        data['patch'] = patch

    def _creer_boutons(self) -> None:
        """Crée les boutons de l'interface."""
        # Créer les axes pour les boutons
        bouton_r_ax: Axes = plt.axes([0.85, 0.75, 0.1, 0.05])
        bouton_n_ax: Axes = plt.axes([0.85, 0.68, 0.1, 0.05])
        bouton_j_ax: Axes = plt.axes([0.85, 0.61, 0.1, 0.05])
        bouton_doute_ax: Axes = plt.axes([0.85, 0.54, 0.1, 0.05])
        bouton_zoom_ax: Axes = plt.axes([0.85, 0.3, 0.1, 0.05])
        bouton_terminer_ax: Axes = plt.axes([0.85, 0.1, 0.1, 0.05])
        bouton_carte_ax: Axes = plt.axes([0.85, 0.42, 0.1, 0.05])

        # Créer les boutons
        self.bouton_r = Button(bouton_r_ax, 'Non Nécrose (R)', color="lightgrey", hovercolor="red")
        self.bouton_r.on_clicked(self._on_button_r_clicked)

        self.bouton_n = Button(bouton_n_ax, 'Nécrose (N)', color="lightgrey", hovercolor="dimgrey")
        self.bouton_n.on_clicked(self._on_button_n_clicked)

        self.bouton_j = Button(bouton_j_ax, 'Autres (J)', color="lightgrey", hovercolor="gold")
        self.bouton_j.on_clicked(self._on_button_j_clicked)

        self.bouton_doute = Button(bouton_doute_ax, 'Douteux (V)', color="lightgrey", hovercolor="mediumorchid")
        self.bouton_doute.on_clicked(self._on_button_doute_clicked)

        self.bouton_carte = Button(bouton_carte_ax, 'Carte (C)', color="lightgrey", hovercolor="limegreen")
        self.bouton_carte.on_clicked(self._on_button_carte_clicked)

        self.bouton_zoom = Button(bouton_zoom_ax, 'Zoom (Ctrl)', color="lightgrey", hovercolor="deepskyblue")
        self.bouton_zoom.on_clicked(self._on_button_zoom_clicked)

        self.bouton_terminer = Button(bouton_terminer_ax, 'Terminé')
        self.bouton_terminer.on_clicked(self._on_button_terminer)

    def _connect_events(self) -> None:
        """Connecte les événements aux gestionnaires."""
        if self.fig:
            self.fig.canvas.mpl_connect('button_press_event', self._on_click)
            self.fig.canvas.mpl_connect('key_press_event', self._on_key_press)
            self.fig.canvas.mpl_connect('button_press_event', self._on_mouse_press)
            self.fig.canvas.mpl_connect('button_release_event', self._on_mouse_release)
            self.fig.canvas.mpl_connect('motion_notify_event', self._on_mouse_motion)

    def _update_buttons_state(self) -> None:
        """Met à jour l'état visuel des boutons."""
        if self.bouton_r and self.bouton_n and self.bouton_j and self.bouton_doute and self.bouton_zoom and self.bouton_carte and self.fig:
            self.bouton_r.color = "red" if self.button_states['r'] else "lightgrey"
            self.bouton_n.color = "dimgrey" if self.button_states['n'] else "lightgrey"
            self.bouton_j.color = "gold" if self.button_states['j'] else "lightgrey"
            self.bouton_doute.color = "mediumorchid" if self.button_states['doute'] else "lightgrey"
            self.bouton_zoom.color = "deepskyblue" if self.button_states['zoom'] else "lightgrey"
            self.bouton_carte.color = "limegreen" if self.button_states['carte'] else "lightgrey"

            self.fig.canvas.draw_idle()

    def _restaurer_grille(self) -> None:
        """Restaure la grille d'images après un zoom ou une carte."""
        if self.image_grande_affichee and self.fig:
            # Supprime l'axe de l'image en grand
            if self.image_grande_ax:
                self.image_grande_ax.remove()
                self.image_grande_ax = None

            # Rendre visible tous les axes de la grille
            for ax in self.image_axes.keys():
                ax.set_visible(True)

            self.image_grande_affichee = False
            print("image_grande_affichee = False")
            self.fig.canvas.draw_idle()

    def _set_active_button(self, button_name: str) -> None:
        """
        Active un bouton et désactive les autres.

        Args :
            button_name (str): Nom du bouton à activer.
        """
        # Restaurer la grille si nécessaire
        self._restaurer_grille()

        # Réinitialiser tous les boutons
        for key in self.button_states:
            self.button_states[key] = False

        # Activer le bouton spécifié
        if button_name in self.button_states:
            self.button_states[button_name] = True

        # Mettre à jour l'affichage des boutons
        self._update_buttons_state()

    # Gestionnaires d'événements pour les boutons
    def _on_button_r_clicked(self, event: Any) -> None:
        self._set_active_button('r')

    def _on_button_n_clicked(self, event: Any) -> None:
        self._set_active_button('n')

    def _on_button_j_clicked(self, event: Any) -> None:
        self._set_active_button('j')

    def _on_button_doute_clicked(self, event: Any) -> None:
        self._set_active_button('doute')

    def _on_button_zoom_clicked(self, event: Any) -> None:
        self._set_active_button('zoom')

    def _on_button_carte_clicked(self, event: Any) -> None:
        self._set_active_button('carte')

        # Si une image est déjà affichée en grand, la faire disparaître
        if self.image_grande_affichee:
            self._restaurer_grille()

        # Chemin du dossier "datasets"
        chemin_datasets: str = os.path.join(os.path.dirname(os.path.realpath('__file__')), "datasets")

        # Obtenir le chemin de l'image avec les plus grandes dimensions
        chemin_image_max: Optional[str] = self.file_manager.trouver_image_plus_grande(chemin_datasets)

        # Vérifier si le fichier existe
        if chemin_image_max and self.fig:
            print(f"L'image avec les dimensions maximales est : {chemin_image_max}")
        else:
            print("Aucune image valide trouvée dans le dossier 'datasets'.")
            return

        # Masquer tous les axes de la grille
        for ax_hidden in self.image_axes.keys():
            ax_hidden.set_visible(False)

        # Ajouter un nouvel axe pour afficher l'image en grand
        if self.fig:
            self.image_grande_ax = self.fig.add_axes([0, 0.1, 0.8, 0.8])
            image_large: Image.Image = Image.open(chemin_image_max)
            self.image_grande_ax.imshow(image_large)
            self.image_grande_ax.axis('off')

            # Mettre à jour l'état
            self.image_grande_affichee = True
            print("image_grande_affichee = True")
            self.fig.canvas.draw_idle()

    def _on_button_terminer(self, event: Any) -> None:
        """Gestionnaire d'événement pour le bouton Terminer."""
        if self.bouton_terminer:
            self.bouton_terminer.set_active(True)  # Réactive le bouton "Terminé"

        # Vérifier si toutes les images ont été sélectionnées
        non_selectionnees: List[Dict[str, Any]] = [data for data in self.image_axes.values() if data['contour_color'] is None]
        if non_selectionnees:
            afficher_boite_dialogue(
                "Erreur",
                "Toutes les images n'ont pas été sélectionnées.\n"
                "Veuillez sélectionner les images restantes, puis réessayez."
            )
        else:
            # Déplacer les images vers les dossiers correspondants
            for data in self.image_axes.values():
                image_chemin: str = data['filepath']
                if data['contour_color']:
                    self.file_manager.deplacer_image(image_chemin, data['contour_color'])

            if self.bouton_terminer:
                self.bouton_terminer.set_active(True)  # Réactive le bouton "Terminé"
            plt.close()

            # Relancer l'affichage avec les images restantes
            self.afficher()

    def _on_click(self, event: Any) -> None:
        """
        Gestionnaire d'événement pour les clics de souris.

        Args :
            event: L'événement de clic.
        """
        if not event.inaxes:
            return

        for ax, data in self.image_axes.items():
            if ax == event.inaxes:  # Si le clic est sur cet axe
                if event.button == 1:  # Clic gauche pour sélectionner
                    if data['patch'] is not None:
                        try:
                            data['patch'].remove()
                        except ValueError:
                            pass

                    # Si le mode zoom est actif
                    if self.button_states['zoom'] and self.fig:
                        # Masquer tous les axes de la grille
                        for ax_hidden in self.image_axes.keys():
                            ax_hidden.set_visible(False)

                        # Ajouter un nouvel axe pour afficher l'image en grand
                        self.image_grande_ax = self.fig.add_axes([0, 0.1, 0.8, 0.8])
                        image_large: Image.Image = Image.open(data['filepath'])
                        self.image_grande_ax.imshow(image_large)
                        self.image_grande_ax.axis('off')

                        # Mettre à jour l'état
                        self.image_grande_affichee = True
                        self.fig.canvas.draw_idle()
                        return

                    # Déterminer la couleur en fonction du bouton actif
                    color: Optional[str] = None
                    if self.button_states['r']:
                        color = "red"
                    elif self.button_states['n']:
                        color = "black"
                    elif self.button_states['j']:
                        color = "yellow"
                    elif self.button_states['doute']:
                        color = "purple"
                    else:
                        return

                    # Vérifier si l'image est déjà marquée avec cette couleur
                    if data.get('contour_color') == color:
                        return

                    # Appliquer le filtre et le contour
                    image_modifiee: Image.Image = data["filtered_images"][color]
                    data['displayed_image'] = image_modifiee
                    data['contour_color'] = color

                    ax.imshow(data['displayed_image'])
                    self._appliquer_contour(ax, data, color)

                    if self.bouton_terminer and self.fig:
                        self.bouton_terminer.set_active(True)
                        self.fig.canvas.draw_idle()

                if event.button == 3 and self.fig:  # Clic droit pour désélectionner
                    if data['patch'] is not None:
                        try:
                            data['patch'].remove()  # Retirer le contour
                        except ValueError:
                            pass

                    # Restaurer l'image originale
                    ax.imshow(data['original_image'])
                    data['displayed_image'] = data['original_image']
                    data['contour_color'] = None
                    data['patch'] = None
                    self.fig.canvas.draw_idle()
                    return

    def _on_mouse_press(self, event: Any) -> None:
        """
        Gestionnaire d'événement pour l'appui sur un bouton de la souris.

        Args :
            event: L'événement d'appui.
        """
        if event.button in (1, 3):  # Bouton gauche ou droit de la souris
            self.mouse_pressed = True
            self.current_ax = event.inaxes

    def _on_mouse_release(self, event: Any) -> None:
        """
        Gestionnaire d'événement pour le relâchement d'un bouton de la souris.

        Args :
            event: L'événement de relâchement.
        """
        self.mouse_pressed = False

    def _on_mouse_motion(self, event: Any) -> None:
        """
        Gestionnaire d'événement pour le mouvement de la souris.

        Args :
            event: L'événement de mouvement.
        """
        if self.mouse_pressed and event.inaxes and self.fig:
            if self.current_ax != event.inaxes:
                self.current_ax = event.inaxes
                ax = event.inaxes
                data = self.image_axes.get(ax, None)

                if data:
                    if event.button == 3:  # Bouton droit de la souris pour désélectionner
                        # Supprimer le contour et restaurer l'image originale
                        if data['patch'] is not None:
                            try:
                                data['patch'].remove()
                            except ValueError:
                                pass  # Le patch a déjà été supprimé
                            data['patch'] = None

                        ax.imshow(data['original_image'])
                        data['displayed_image'] = data['original_image']
                        data['contour_color'] = None

                        # Limiter les appels à draw_idle pour améliorer les performances
                        current_time: float = time.time()
                        if current_time - self.last_update_time > 0.10:
                            self.fig.canvas.draw_idle()
                            self.last_update_time = current_time
                        return  # Fin de la désélection

                    # Déterminer la couleur souhaitée en fonction des boutons actifs
                    color: Optional[str] = None
                    if self.button_states['r']:
                        color = "red"
                    elif self.button_states['n']:
                        color = "black"
                    elif self.button_states['j']:
                        color = "yellow"
                    elif self.button_states['doute']:
                        color = "purple"
                    else:
                        return  # Aucun bouton actif, ne rien faire

                    # Vérifier si l'image est déjà marquée avec la couleur souhaitée
                    if data.get('contour_color') == color:
                        return  # La couleur est déjà appliquée, ne rien faire

                    # Mettre à jour l'image avec la version filtrée pré-générée
                    ax.imshow(data['filtered_images'][color])
                    data['displayed_image'] = data['filtered_images'][color]
                    data['contour_color'] = color

                    # Supprimer le patch précédent s'il existe
                    if data['patch'] is not None:
                        try:
                            data['patch'].remove()
                        except ValueError:
                            pass  # Le patch a déjà été supprimé

                    # Ajouter un nouveau patch avec la couleur souhaitée
                    patch = patches.Rectangle(
                        (0, 0),
                        ax.get_xlim()[1],
                        ax.get_ylim()[0],
                        linewidth=5,
                        edgecolor=color,
                        facecolor='none'
                    )
                    ax.add_patch(patch)
                    data['patch'] = patch

                    # Activer le bouton "Terminé"
                    if self.bouton_terminer:
                        self.bouton_terminer.set_active(True)

                    # Limiter les appels à draw_idle pour améliorer les performances
                    current_time: float = time.time()
                    if current_time - self.last_update_time > 0.10:
                        self.fig.canvas.draw_idle()
                        self.last_update_time = current_time

    def _on_key_press(self, event: Any) -> None:
        """
        Gestionnaire d'événement pour l'appui sur une touche.

        Args :
            event: L'événement d'appui sur une touche.
        """
        if event.key == 'r':
            self._set_active_button('r')
        elif event.key == 'n':
            self._set_active_button('n')
        elif event.key == 'j':
            self._set_active_button('j')
        elif event.key == 'v':
            self._set_active_button('doute')
        elif event.key == 'control':
            self._set_active_button('zoom')
        elif event.key == 'c':
            self._set_active_button('carte')
            self._on_button_carte_clicked(event)

    def afficher(self) -> None:
        """Affiche la grille d'images."""
        if self._initialiser_interface():
            plt.show()
        else:
            # Si _initialiser_interface() retourne False, cela signifie qu'il n'y a plus d'images à traiter
            # Fermer proprement l'application
            print("Toutes les images ont été traitées. Fermeture de l'application.")
            sys.exit(0)


#############################################
# Point d'entrée principal
#############################################

def main() -> None:
    """
    Fonction principale qui initialise et lance l'application.

    Usage:
        python viewer_standalone.py <dossier_images> <dossier_modele>
    """
    # Vérifier les arguments
    if len(sys.argv) == 3:
        # Récupérer les arguments
        dossier_images: str = os.path.join(sys.argv[1], "datasets")
        dossier_modele: str = sys.argv[2]

        # Initialiser les composants
        file_manager: FileManager = FileManager(dossier_modele)
        image_processor: ImageProcessor = ImageProcessor()

        # Créer et afficher le visualiseur d'images
        viewer: ImageViewer = ImageViewer(dossier_images, file_manager, image_processor)
        viewer.afficher()
    else:
        print("Usage: python viewer_standalone.py <dossier_images> <dossier_modele>")
        print("Exemple: python viewer_standalone.py C:\\chemin\\vers\\dossier C:\\chemin\\vers\\modele")
        sys.exit(1)


if __name__ == "__main__":
    main()