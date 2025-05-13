import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, Dataset
from torchvision import transforms, models
from PIL import Image
import os
import sys

# Transformation et DataLoader
transform = transforms.Compose([
    transforms.Resize((224, 224)),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
])


class TumorDataset(Dataset):
    def __init__(self, image_paths, labels, transform=None):
        self.image_paths = image_paths
        self.labels = labels
        self.transform = transform

    def __len__(self):
        return len(self.image_paths)

    def __getitem__(self, idx):
        image = Image.open(self.image_paths[idx]).convert('RGB')
        label = self.labels[idx]

        if self.transform:
            image = self.transform(image)

        return image, label


def load_train_data():
    NAS_Path = sys.argv[1]
    # Chemin du dossier contenant les sous-dossiers 'Necrotic', 'Viable', et 'Other'
    subfolder_path_viable = NAS_Path + '/Viable'
    subfolder_path_necroses = NAS_Path + '/Necrotic'
    subfolder_path_other = NAS_Path + '/Other'

    # Chargement des chemins des images "Viable"
    file_names_viable = os.listdir(subfolder_path_viable)
    nb_images_viable = count_files_in_directory(subfolder_path_viable)
    train_image_paths = [
        os.path.join(subfolder_path_viable, file_names_viable[i]) for i in
        range(nb_images_viable)]
    train_labels = ([0] * nb_images_viable)  # Étiquette 0 pour 'Viable'

    # Chargement des chemins des images "Necrotic"
    file_names_necrotic = os.listdir(subfolder_path_necroses)
    nb_images_necrotic = count_files_in_directory(subfolder_path_necroses)
    train_image_paths.extend(
        [os.path.join(subfolder_path_necroses, file_names_necrotic[i]) for i in
         range(nb_images_necrotic)])
    train_labels.extend([1] * nb_images_necrotic)  # Étiquette 1 pour 'Necrotic'

    # Chargement des chemins des images "Other"
    file_names_other = os.listdir(subfolder_path_other)
    nb_images_other = count_files_in_directory(subfolder_path_other)
    train_image_paths.extend(
        [os.path.join(subfolder_path_other, file_names_other[i]) for i in
         range(nb_images_other)])
    train_labels.extend([2] * nb_images_other)  # Étiquette 2 pour 'Other'

    return train_image_paths, train_labels


def create_dataloader(image_paths, labels, batch_size=16, shuffle=True):
    dataset = TumorDataset(image_paths, labels, transform=transform)
    dataloader = DataLoader(dataset, batch_size=batch_size, shuffle=shuffle)
    return dataloader


def train_model(model, dataloader, criterion, optimizer, num_epochs=5):
    for epoch in range(num_epochs):
        model.train()
        epoch_loss = 0
        correct = 0
        total = 0

        for images, labels in dataloader:
            optimizer.zero_grad()
            outputs = model(images)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()

            epoch_loss += loss.item()
            _, predicted = outputs.max(1)
            total += labels.size(0)
            correct += predicted.eq(labels).sum().item()

        print(
            f'Epoch {epoch + 1}/{num_epochs}, Loss: {epoch_loss / len(dataloader)}, Accuracy: {100. * correct / total:.2f}%')


def save_model(model, path):
    torch.save(model.state_dict(), path)


def count_files_in_directory(folder_path):
    # Vérifiez si le chemin est un répertoire    
    if not os.path.isdir(folder_path):
        raise ValueError("Le chemin spécifié n'est pas un répertoire.")

    # Liste des fichiers dans le répertoire
    files = [f for f in os.listdir(folder_path) if
             os.path.isfile(os.path.join(folder_path, f))]

    # Retourner le nombre de fichiers
    return len(files)


def main():
    # Charger les données d'entraînement
    train_image_paths, train_labels = load_train_data()

    # Créer le DataLoader
    dataloader = create_dataloader(train_image_paths, train_labels)

    # Initialiser le modèle
    model = models.resnet18(weights=None)
    model.fc = nn.Linear(model.fc.in_features, 3)  # 3 classes

    # Définir la fonction de perte avec les poids des classes
    criterion = nn.CrossEntropyLoss()

    # Définir l'optimiseur
    optimizer = optim.Adam(model.parameters(), lr=1e-4)

    # Entraîner le modèle
    train_model(model, dataloader, criterion, optimizer, num_epochs=10)

    # Sauvegarder le modèle
    save_model(model, sys.argv[1] + '/model.pth')


if __name__ == '__main__':
    main()
